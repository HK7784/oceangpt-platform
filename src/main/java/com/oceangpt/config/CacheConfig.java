package com.oceangpt.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 缓存配置类
 * 配置应用程序的缓存策略和管理器
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 主缓存管理器
     * 使用ConcurrentMapCacheManager进行内存缓存
     * 当未配置Redis时生效
     */
    @Bean
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "spring.cache.type", 
        havingValue = "simple", 
        matchIfMissing = true
    )
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 预定义缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "predictions",        // 预测结果缓存
            "prediction-cache",   // 预测缓存（CacheService使用）
            "noaaData",          // NOAA数据缓存
            "nutrientData",      // 营养盐数据缓存
            "aggregatedData",    // 聚合数据缓存
            "modelInfo",         // 模型信息缓存
            "qualityReports",    // 数据质量报告缓存
            "correlations",      // 相关性分析缓存
            "trends"             // 趋势分析缓存
        ));
        
        // 允许运行时创建新缓存
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}
