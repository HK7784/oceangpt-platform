package com.oceangpt.service;

import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import com.oceangpt.dto.SatelliteDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 缓存服务
 * 提供预测结果和卫星数据的缓存功能
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    // 内存缓存，生产环境建议使用Redis
    private final Map<String, CachedPredictionResult> predictionCache = new ConcurrentHashMap<>();
    private final Map<String, CachedSatelliteData> satelliteDataCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（小时）
    private static final int PREDICTION_CACHE_HOURS = 24;
    private static final int SATELLITE_DATA_CACHE_HOURS = 6;
    
    /**
     * 获取缓存的预测结果
     * 
     * @param request 预测请求
     * @return 缓存的预测结果，如果不存在或已过期则返回null
     */
    @Cacheable(value = "prediction-cache", key = "#request.latitude + '_' + #request.longitude + '_' + #request.predictionTime", unless = "#result == null")
    public PredictionResponse getCachedPrediction(PredictionRequest request) {
        String cacheKey = generatePredictionCacheKey(request);
        CachedPredictionResult cached = predictionCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired(PREDICTION_CACHE_HOURS)) {
            logger.debug("从缓存获取预测结果: {}", cacheKey);
            return cached.getResult();
        }
        
        // 清理过期缓存
        if (cached != null && cached.isExpired(PREDICTION_CACHE_HOURS)) {
            predictionCache.remove(cacheKey);
            logger.debug("清理过期预测缓存: {}", cacheKey);
        }
        
        return null;
    }
    
    /**
     * 缓存预测结果
     * 
     * @param request 预测请求
     * @param response 预测响应
     */
    public void cachePrediction(PredictionRequest request, PredictionResponse response) {
        if (request == null || response == null || !response.isSuccess()) {
            return;
        }
        
        String cacheKey = generatePredictionCacheKey(request);
        CachedPredictionResult cached = new CachedPredictionResult(response, LocalDateTime.now());
        predictionCache.put(cacheKey, cached);
        
        logger.debug("缓存预测结果: {}", cacheKey);
    }
    
    /**
     * 获取缓存的卫星数据
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param date 日期
     * @return 缓存的卫星数据，如果不存在或已过期则返回null
     */
    @Cacheable(value = "satellite-data", key = "#latitude + '_' + #longitude + '_' + #date")
    public SatelliteDataResponse getCachedSatelliteData(Double latitude, Double longitude, String date) {
        String cacheKey = generateSatelliteDataCacheKey(latitude, longitude, date);
        CachedSatelliteData cached = satelliteDataCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired(SATELLITE_DATA_CACHE_HOURS)) {
            logger.debug("从缓存获取卫星数据: {}", cacheKey);
            return cached.getData();
        }
        
        // 清理过期缓存
        if (cached != null && cached.isExpired(SATELLITE_DATA_CACHE_HOURS)) {
            satelliteDataCache.remove(cacheKey);
            logger.debug("清理过期卫星数据缓存: {}", cacheKey);
        }
        
        return null;
    }
    
    /**
     * 缓存卫星数据
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param date 日期
     * @param data 卫星数据
     */
    public void cacheSatelliteData(Double latitude, Double longitude, String date, SatelliteDataResponse data) {
        if (latitude == null || longitude == null || date == null || data == null || !data.isSuccess()) {
            return;
        }
        
        String cacheKey = generateSatelliteDataCacheKey(latitude, longitude, date);
        CachedSatelliteData cached = new CachedSatelliteData(data, LocalDateTime.now());
        satelliteDataCache.put(cacheKey, cached);
        
        logger.debug("缓存卫星数据: {}", cacheKey);
    }
    
    /**
     * 清理所有缓存
     */
    @CacheEvict(value = {"prediction-cache", "satellite-data"}, allEntries = true)
    public void clearAllCache() {
        predictionCache.clear();
        satelliteDataCache.clear();
        logger.info("清理所有缓存");
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        // 清理过期的预测缓存
        predictionCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(PREDICTION_CACHE_HOURS);
            if (expired) {
                logger.debug("清理过期预测缓存: {}", entry.getKey());
            }
            return expired;
        });
        
        // 清理过期的卫星数据缓存
        satelliteDataCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(SATELLITE_DATA_CACHE_HOURS);
            if (expired) {
                logger.debug("清理过期卫星数据缓存: {}", entry.getKey());
            }
            return expired;
        });
        
        logger.info("清理过期缓存完成。预测缓存: {}, 卫星数据缓存: {}", 
            predictionCache.size(), satelliteDataCache.size());
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            predictionCache.size(),
            satelliteDataCache.size(),
            countValidPredictionCache(),
            countValidSatelliteDataCache()
        );
    }
    
    /**
     * 生成预测缓存键
     */
    private String generatePredictionCacheKey(PredictionRequest request) {
        return String.format("pred_%.4f_%.4f_%s", 
            request.getLatitude(), 
            request.getLongitude(),
            request.getPredictionTime() != null ? 
                request.getPredictionTime().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "now");
    }
    
    /**
     * 生成卫星数据缓存键
     */
    private String generateSatelliteDataCacheKey(Double latitude, Double longitude, String date) {
        return String.format("sat_%.4f_%.4f_%s", latitude, longitude, date);
    }
    
    /**
     * 统计有效的预测缓存数量
     */
    private int countValidPredictionCache() {
        return (int) predictionCache.values().stream()
            .filter(cached -> !cached.isExpired(PREDICTION_CACHE_HOURS))
            .count();
    }
    
    /**
     * 统计有效的卫星数据缓存数量
     */
    private int countValidSatelliteDataCache() {
        return (int) satelliteDataCache.values().stream()
            .filter(cached -> !cached.isExpired(SATELLITE_DATA_CACHE_HOURS))
            .count();
    }
    
    /**
     * 缓存的预测结果
     */
    private static class CachedPredictionResult {
        private final PredictionResponse result;
        private final LocalDateTime cacheTime;
        
        public CachedPredictionResult(PredictionResponse result, LocalDateTime cacheTime) {
            this.result = result;
            this.cacheTime = cacheTime;
        }
        
        public PredictionResponse getResult() {
            return result;
        }
        
        public boolean isExpired(int hours) {
            return LocalDateTime.now().isAfter(cacheTime.plusHours(hours));
        }
    }
    
    /**
     * 缓存的卫星数据
     */
    private static class CachedSatelliteData {
        private final SatelliteDataResponse data;
        private final LocalDateTime cacheTime;
        
        public CachedSatelliteData(SatelliteDataResponse data, LocalDateTime cacheTime) {
            this.data = data;
            this.cacheTime = cacheTime;
        }
        
        public SatelliteDataResponse getData() {
            return data;
        }
        
        public boolean isExpired(int hours) {
            return LocalDateTime.now().isAfter(cacheTime.plusHours(hours));
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final int totalPredictionCache;
        private final int totalSatelliteDataCache;
        private final int validPredictionCache;
        private final int validSatelliteDataCache;
        
        public CacheStats(int totalPredictionCache, int totalSatelliteDataCache, 
                         int validPredictionCache, int validSatelliteDataCache) {
            this.totalPredictionCache = totalPredictionCache;
            this.totalSatelliteDataCache = totalSatelliteDataCache;
            this.validPredictionCache = validPredictionCache;
            this.validSatelliteDataCache = validSatelliteDataCache;
        }
        
        public int getTotalPredictionCache() {
            return totalPredictionCache;
        }
        
        public int getTotalSatelliteDataCache() {
            return totalSatelliteDataCache;
        }
        
        public int getValidPredictionCache() {
            return validPredictionCache;
        }
        
        public int getValidSatelliteDataCache() {
            return validSatelliteDataCache;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{预测缓存: %d/%d, 卫星数据缓存: %d/%d}", 
                validPredictionCache, totalPredictionCache,
                validSatelliteDataCache, totalSatelliteDataCache);
        }
    }
}