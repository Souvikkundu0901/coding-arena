package com.codingarena.config;

import com.codingarena.submission.client.Judge0Client;
import com.codingarena.submission.client.Judge0Response;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;

import java.lang.reflect.Proxy;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new RedisConnectionFactory() {
            @Override
            public RedisConnection getConnection() {
                return (RedisConnection) Proxy.newProxyInstance(
                        RedisConnection.class.getClassLoader(),
                        new Class<?>[]{RedisConnection.class},
                        (proxy, method, args) -> {
                            if ("isPipelined".equals(method.getName()) || "isQueueing".equals(method.getName())) {
                                return false;
                            }
                            if (method.getReturnType().equals(boolean.class) || method.getReturnType().equals(Boolean.class)) {
                                return false;
                            }
                            if (method.getReturnType().equals(long.class) || method.getReturnType().equals(Long.class)) {
                                return 0L;
                            }
                            if (method.getReturnType().equals(double.class) || method.getReturnType().equals(Double.class)) {
                                return 0.0;
                            }
                            return null;
                        }
                );
            }

            @Override
            public RedisClusterConnection getClusterConnection() {
                return null;
            }

            @Override
            public boolean getConvertPipelineAndTxResults() {
                return false;
            }

            @Override
            public RedisSentinelConnection getSentinelConnection() {
                return null;
            }

            @Override
            public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
                return null;
            }
        };
    }

    @Bean
    @Primary
    public Judge0Client judge0Client() {
        return new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", false) {
            @Override
            public Judge0Response execute(String code, String language, String stdin, String expectedOutput) {
                Judge0Response response = new Judge0Response();
                response.setStatus(new Judge0Response.Judge0Status(3, "Accepted"));
                response.setStdout(expectedOutput != null ? expectedOutput : "");
                return response;
            }
        };
    }
}
