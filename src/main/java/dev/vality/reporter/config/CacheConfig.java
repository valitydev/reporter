package dev.vality.reporter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import dev.vality.reporter.config.properties.DominantCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final DominantCacheProperties dominantCacheProperties;

    @Bean
    public CacheManager currenciesCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(getCacheConfig(dominantCacheProperties.getCurrencies()));
        caffeineCacheManager.setCacheNames(List.of("currencies"));
        return caffeineCacheManager;
    }

    private Caffeine getCacheConfig(DominantCacheProperties.CacheConfig cacheConfig) {
        return Caffeine.newBuilder()
                .expireAfterAccess(cacheConfig.getTtlSec(), TimeUnit.SECONDS)
                .maximumSize(cacheConfig.getPoolSize());
    }

}
