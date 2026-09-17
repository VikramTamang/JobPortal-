package com.jobportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * A hand-built RedisCacheManager rather than relying on Boot's
 * spring.cache.redis.* auto-config, because each cache here needs its own
 * TTL: job details can sit for 15 minutes (changes are infrequent, evicted
 * explicitly on mutation anyway), search results only 60 seconds (there
 * are too many filter combinations to evict individually, so a short TTL
 * bounds staleness instead), company info 30 minutes.
 *
 * JSON serialization (not the JDK default) is deliberate: JDK
 * serialization requires every cached class to implement Serializable,
 * breaks the moment a field's type changes shape, and produces binary blobs
 * that are unreadable if you ever need to inspect Redis directly for
 * debugging. JSON costs a little more CPU per (de)serialization but is far
 * more robust and debuggable.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Needed so the serializer records each value's concrete runtime
        // class (e.g. PageImpl, JobResponse) and can deserialize back to
        // the same type -- without this, Jackson only knows the generic
        // Object/Page interface shape and reconstruction fails.
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        var jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                "jobDetails", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                "jobListings", defaultConfig.entryTtl(Duration.ofMinutes(2)),
                "jobSearch", defaultConfig.entryTtl(Duration.ofSeconds(60)),
                "companyInfo", defaultConfig.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}