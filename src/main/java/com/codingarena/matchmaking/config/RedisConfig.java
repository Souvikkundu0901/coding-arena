package com.codingarena.matchmaking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisScript<List> matchPairingScript() {
        String script =
                "local count = redis.call('ZCARD', KEYS[1])\n" +
                "if count < 2 then\n" +
                "    return {}\n" +
                "end\n" +
                "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES')\n" +
                "if #members < 4 then\n" +
                "    return {}\n" +
                "end\n" +
                "local min_diff = 99999999\n" +
                "local best_idx = 1\n" +
                "for i = 1, #members - 3, 2 do\n" +
                "    local rating1 = tonumber(members[i+1])\n" +
                "    local rating2 = tonumber(members[i+3])\n" +
                "    local diff = math.abs(rating2 - rating1)\n" +
                "    if diff < min_diff then\n" +
                "        min_diff = diff\n" +
                "        best_idx = i\n" +
                "    end\n" +
                "end\n" +
                "local user1 = members[best_idx]\n" +
                "local user2 = members[best_idx + 2]\n" +
                "redis.call('ZREM', KEYS[1], user1, user2)\n" +
                "return {user1, user2}";

        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(List.class);
        return redisScript;
    }
}
