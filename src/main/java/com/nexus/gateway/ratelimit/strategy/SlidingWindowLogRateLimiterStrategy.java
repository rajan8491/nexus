package com.nexus.gateway.ratelimit.strategy;

import com.nexus.gateway.domain.RateLimitRuleDefinition;
import com.nexus.gateway.ratelimit.Algorithm;
import com.nexus.gateway.ratelimit.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlidingWindowLogRateLimiterStrategy implements RateLimiterStrategy {

    private static final String SCRIPT = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local member_id = ARGV[4]
            
                local clear_before = now - (window * 1000)
                redis.call('ZREMRANGEBYSCORE', key, 0, clear_before)
            
                local current_count = redis.call('ZCARD', key)
                local allowed = 0
                local remaining = 0
            
                if current_count < limit then
                    redis.call('ZADD', key, now, member_id)
                    allowed = 1
                    remaining = limit - (current_count + 1)
                else
                    remaining = 0
                end
            
                redis.call('EXPIRE', key, window)
                return { allowed, remaining, window }
            """;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> redisScript = new DefaultRedisScript<>(SCRIPT, List.class);

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public Mono<RateLimitResult> isAllowed(String clientKey, RateLimitRuleDefinition config) {
        String key = "rl:sw:" + config.id() + ":" + clientKey;
        long nowMillis = Instant.now().toEpochMilli();
        String memberId = UUID.randomUUID().toString();

        return redisTemplate.execute(redisScript,
                List.of(key),
                List.of(String.valueOf(nowMillis), String.valueOf(config.windowSeconds()), String.valueOf(config.capacity()), memberId)
        ).next().map(res -> new RateLimitResult(
                (Long) res.get(0) == 1L,
                (Long) res.get(1),
                (Long) res.get(2)
        ));
    }
}