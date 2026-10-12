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

import java.util.List;

@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiterStrategy implements RateLimiterStrategy {

    private static final String SCRIPT = """
                local key = KEYS[1]
                local limit = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
            
                local current = redis.call('INCR', key)
                if current == 1 then
                    redis.call('EXPIRE', key, window)
                end
            
                local ttl = redis.call('TTL', key)
                if current > limit then
                    return { 0, 0, ttl }
                else
                    return { 1, limit - current, ttl }
                end
            """;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> redisScript = new DefaultRedisScript<>(SCRIPT, List.class);

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.FIXED_WINDOW;
    }

    @Override
    public Mono<RateLimitResult> isAllowed(String clientKey, RateLimitRuleDefinition config) {
        String key = "rl:fw:" + config.id() + ":" + clientKey;

        return redisTemplate.execute(
                redisScript,
                List.of(key),
                List.of(String.valueOf(config.capacity()), String.valueOf(config.windowSeconds()))
        ).next().map(res -> new RateLimitResult(
                (Long) res.get(0) == 1L,
                (Long) res.get(1),
                (Long) res.get(2)
        ));
    }
}