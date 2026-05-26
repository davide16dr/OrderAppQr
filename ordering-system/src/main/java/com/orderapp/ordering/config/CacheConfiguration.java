package com.orderapp.ordering.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * Configurazione del caching per l'applicazione.
 * 
 * - In-memory caching in development mode
 * - Redis caching in production mode
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    /**
     * Cache manager per development con Map in-memory
     */
    @Bean
    @Profile("!prod")
    public CacheManager cacheManager() {
        // Cache in-memory per development/test
        return new ConcurrentMapCacheManager(
            // Admin Tenant
            "allTenants", "tenantById",
            // Areas
            "tenantAreas", "tenantArea",
            // Categories
            "tenantCategories", "tenantCategory"
        );
    }

    /**
     * Cache manager per production con Redis
     */
    @Bean
    @Profile("prod")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Configurazione Redis con TTL di 1 ora (3600 secondi)
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(java.time.Duration.ofSeconds(3600))
                .disableCachingNullValues();

        return RedisCacheManager.create(connectionFactory);
    }
}
