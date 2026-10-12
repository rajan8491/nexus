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

@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiterStrategy implements RateLimiterStrategy {

    private static final String SCRIPT = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local refill_rate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
            
                local bucket = redis.call('HMGET', key, 'tokens', 'last_refreshed')
                local tokens = tonumber(bucket[1])
                local last_refreshed = tonumber(bucket[2])
            
                if tokens == nil then
                    tokens = capacity
                    last_refreshed = now
                end
            
                local delta = math.max(0, now - last_refreshed)
                local filled = math.min(capacity, tokens + (delta * refill_rate))
            
                local allowed = 0
                if filled >= 1 then
                    allowed = 1
                    filled = filled - 1
                end
            
                redis.call('HMSET', key, 'tokens', filled, 'last_refreshed', now)
                redis.call('EXPIRE', key, math.ceil(capacity / refill_rate) * 2)
            
                return { allowed, filled, 0 }
            """;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> redisScript = new DefaultRedisScript<>(SCRIPT, List.class);

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.TOKEN_BUCKET;
    }

    @Override
    public Mono<RateLimitResult> isAllowed(String clientKey, RateLimitRuleDefinition config) {
        String key = "rl:tb:" + config.id() + ":" + clientKey;
        long now = Instant.now().getEpochSecond();

        return redisTemplate.execute(redisScript,
                List.of(key),
                List.of(String.valueOf(config.capacity()), String.valueOf(config.refillRate()), String.valueOf(now))
        ).next().map(res -> new RateLimitResult(
                (Long) res.get(0) == 1L,
                (Long) res.get(1),
                (Long) res.get(2)
        ));
    }
}