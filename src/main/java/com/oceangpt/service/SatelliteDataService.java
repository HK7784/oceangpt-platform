package com.oceangpt.service;

import com.oceangpt.dto.SatelliteDataRequest;
import com.oceangpt.dto.SatelliteDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 卫星数据获取服务
 * 负责从Sentinel-2/3卫星数据源获取光谱反射率数据
 */
@Service
public class SatelliteDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(SatelliteDataService.class);
    
    @Autowired
    private CacheService cacheService;
    
    @Value("${oceangpt.satellite.sentinel2.api.url:}")
    private String sentinel2ApiUrl;
    
    @Value("${oceangpt.satellite.sentinel3.api.url:}")
    private String sentinel3ApiUrl;
    
    @Value("${oceangpt.satellite.api.key:}")
    private String apiKey;
    
    @Value("${oceangpt.satellite.timeout:30000}")
    private int timeoutMs;
    
    @Value("${oceangpt.satellite.mock.enabled:true}")
    private boolean mockEnabled;
    
    private final RestTemplate restTemplate;
    private final Map<String, SatelliteDataResponse> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1); // 1小时缓存
    
    public SatelliteDataService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 获取指定位置和时间的卫星数据
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param dateTime 时间
     * @return 卫星数据响应
     */
    @Cacheable(value = "satellite-data", key = "#latitude + '_' + #longitude + '_' + #dateTime")
    public SatelliteDataResponse getSatelliteData(Double latitude, Double longitude, LocalDateTime dateTime) {
        try {
            logger.info("获取卫星数据: 经度={}, 纬度={}, 时间={}", longitude, latitude, dateTime);
            
            // 首先尝试从CacheService获取
            String dateKey = dateTime != null ? dateTime.toString() : "now";
            SatelliteDataResponse cachedData = cacheService.getCachedSatelliteData(
                latitude, longitude, dateKey);
            
            if (cachedData != null) {
                logger.info("从CacheService获取卫星数据");
                return cachedData;
            }
            
            // 检查本地缓存
            String cacheKey = generateCacheKey(latitude, longitude, dateTime);
            SatelliteDataResponse localCachedData = getCachedData(cacheKey);
            if (localCachedData != null) {
                logger.info("使用本地缓存的卫星数据");
                return localCachedData;
            }
            
            SatelliteDataResponse response;
            
            if (mockEnabled || isApiConfigurationMissing()) {
                logger.info("使用模拟卫星数据");
                response = generateMockSatelliteData(latitude, longitude, dateTime);
            } else {
                logger.info("从真实API获取卫星数据");
                response = fetchRealSatelliteData(latitude, longitude, dateTime);
            }
            
            // 缓存结果到CacheService和本地缓存
            if (response != null && response.isSuccess()) {
                cacheService.cacheSatelliteData(
                    latitude, longitude, dateKey, response);
                cacheData(cacheKey, response);
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("获取卫星数据失败: {}", e.getMessage(), e);
            // 返回模拟数据作为备选
            return generateMockSatelliteData(latitude, longitude, dateTime);
        }
    }
    
    /**
     * 从真实API获取卫星数据
     */
    private SatelliteDataResponse fetchRealSatelliteData(Double latitude, Double longitude, LocalDateTime dateTime) {
        try {
            // 创建请求
            SatelliteDataRequest request = new SatelliteDataRequest();
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setDateTime(dateTime);
            request.setApiKey(apiKey);
            
            SatelliteDataResponse response = new SatelliteDataResponse();
            response.setSuccess(true);
            response.setLatitude(latitude);
            response.setLongitude(longitude);
            response.setDateTime(dateTime);
            
            // 获取Sentinel-2数据
            Map<String, Double> s2Data = fetchSentinel2Data(request);
            response.setS2Data(s2Data);
            
            // 获取Sentinel-3数据
            Map<String, Double> s3Data = fetchSentinel3Data(request);
            response.setS3Data(s3Data);
            
            // 计算神经网络预测值
            response.setChlNN(calculateChlNN(s2Data, s3Data));
            response.setTsmNN(calculateTsmNN(s2Data, s3Data));
            
            response.setDataSource("Sentinel-2/3 API");
            response.setQualityScore(0.95);
            
            logger.info("成功获取真实卫星数据");
            return response;
            
        } catch (Exception e) {
            logger.error("从真实API获取卫星数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("卫星数据获取失败", e);
        }
    }
    
    /**
     * 获取Sentinel-2数据
     */
    private Map<String, Double> fetchSentinel2Data(SatelliteDataRequest request) {
        try {
            if (sentinel2ApiUrl == null || sentinel2ApiUrl.isEmpty()) {
                throw new IllegalStateException("Sentinel-2 API URL未配置");
            }
            
            // 构建API请求URL
            String url = String.format("%s?lat=%f&lon=%f&date=%s&key=%s",
                sentinel2ApiUrl,
                request.getLatitude(),
                request.getLongitude(),
                request.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE),
                request.getApiKey());
            
            // 调用API
            @SuppressWarnings("unchecked")
            Map<String, Object> apiResponse = restTemplate.getForObject(url, Map.class);
            
            if (apiResponse == null || !"success".equals(apiResponse.get("status"))) {
                throw new RuntimeException("Sentinel-2 API返回错误");
            }
            
            // 解析光谱数据
            @SuppressWarnings("unchecked")
            Map<String, Object> spectralData = (Map<String, Object>) apiResponse.get("spectral_data");
            
            Map<String, Double> s2Data = new HashMap<>();
            s2Data.put("B2", getDoubleValue(spectralData, "B2"));
            s2Data.put("B3", getDoubleValue(spectralData, "B3"));
            s2Data.put("B4", getDoubleValue(spectralData, "B4"));
            s2Data.put("B5", getDoubleValue(spectralData, "B5"));
            s2Data.put("B6", getDoubleValue(spectralData, "B6"));
            s2Data.put("B7", getDoubleValue(spectralData, "B7"));
            s2Data.put("B8", getDoubleValue(spectralData, "B8"));
            s2Data.put("B8A", getDoubleValue(spectralData, "B8A"));
            
            return s2Data;
            
        } catch (RestClientException e) {
            logger.error("Sentinel-2 API调用失败: {}", e.getMessage());
            throw new RuntimeException("Sentinel-2数据获取失败", e);
        }
    }
    
    /**
     * 获取Sentinel-3数据
     */
    private Map<String, Double> fetchSentinel3Data(SatelliteDataRequest request) {
        try {
            if (sentinel3ApiUrl == null || sentinel3ApiUrl.isEmpty()) {
                throw new IllegalStateException("Sentinel-3 API URL未配置");
            }
            
            // 构建API请求URL
            String url = String.format("%s?lat=%f&lon=%f&date=%s&key=%s",
                sentinel3ApiUrl,
                request.getLatitude(),
                request.getLongitude(),
                request.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE),
                request.getApiKey());
            
            // 调用API
            @SuppressWarnings("unchecked")
            Map<String, Object> apiResponse = restTemplate.getForObject(url, Map.class);
            
            if (apiResponse == null || !"success".equals(apiResponse.get("status"))) {
                throw new RuntimeException("Sentinel-3 API返回错误");
            }
            
            // 解析光谱数据
            @SuppressWarnings("unchecked")
            Map<String, Object> spectralData = (Map<String, Object>) apiResponse.get("spectral_data");
            
            Map<String, Double> s3Data = new HashMap<>();
            s3Data.put("Oa01", getDoubleValue(spectralData, "Oa01"));
            s3Data.put("Oa02", getDoubleValue(spectralData, "Oa02"));
            s3Data.put("Oa03", getDoubleValue(spectralData, "Oa03"));
            s3Data.put("Oa04", getDoubleValue(spectralData, "Oa04"));
            s3Data.put("Oa05", getDoubleValue(spectralData, "Oa05"));
            s3Data.put("Oa06", getDoubleValue(spectralData, "Oa06"));
            s3Data.put("Oa07", getDoubleValue(spectralData, "Oa07"));
            s3Data.put("Oa08", getDoubleValue(spectralData, "Oa08"));
            
            return s3Data;
            
        } catch (RestClientException e) {
            logger.error("Sentinel-3 API调用失败: {}", e.getMessage());
            throw new RuntimeException("Sentinel-3数据获取失败", e);
        }
    }
    
    /**
     * 生成模拟卫星数据
     */
    private SatelliteDataResponse generateMockSatelliteData(Double latitude, Double longitude, LocalDateTime dateTime) {
        logger.info("生成模拟卫星数据");
        
        SatelliteDataResponse response = new SatelliteDataResponse();
        response.setSuccess(true);
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setDateTime(dateTime);
        response.setDataSource("Mock Data");
        response.setQualityScore(0.85);
        
        // 生成基于地理位置的合理模拟数据
        Map<String, Double> s2Data = generateMockSentinel2Data(latitude, longitude);
        Map<String, Double> s3Data = generateMockSentinel3Data(latitude, longitude);
        
        response.setS2Data(s2Data);
        response.setS3Data(s3Data);
        
        // 计算神经网络预测值
        response.setChlNN(calculateChlNN(s2Data, s3Data));
        response.setTsmNN(calculateTsmNN(s2Data, s3Data));
        
        return response;
    }
    
    /**
     * 生成模拟Sentinel-2数据
     */
    private Map<String, Double> generateMockSentinel2Data(Double latitude, Double longitude) {
        Map<String, Double> s2Data = new HashMap<>();
        
        // 基于地理位置生成合理的光谱反射率值
        double baseReflectance = 0.05 + (latitude + 90) / 180 * 0.02; // 基于纬度的基础反射率
        double longitudeEffect = Math.sin(Math.toRadians(longitude)) * 0.01; // 经度影响
        
        s2Data.put("B2", Math.max(0.01, baseReflectance + longitudeEffect + Math.random() * 0.01)); // 蓝光
        s2Data.put("B3", Math.max(0.01, baseReflectance * 0.9 + longitudeEffect + Math.random() * 0.01)); // 绿光
        s2Data.put("B4", Math.max(0.01, baseReflectance * 0.8 + longitudeEffect + Math.random() * 0.01)); // 红光
        s2Data.put("B5", Math.max(0.01, baseReflectance * 0.7 + longitudeEffect + Math.random() * 0.01)); // 红边
        s2Data.put("B6", Math.max(0.01, baseReflectance * 0.6 + longitudeEffect + Math.random() * 0.01)); // 红边
        s2Data.put("B7", Math.max(0.01, baseReflectance * 0.5 + longitudeEffect + Math.random() * 0.01)); // 红边
        s2Data.put("B8", Math.max(0.01, baseReflectance * 0.4 + longitudeEffect + Math.random() * 0.01)); // 近红外
        s2Data.put("B8A", Math.max(0.01, baseReflectance * 0.45 + longitudeEffect + Math.random() * 0.01)); // 近红外
        
        return s2Data;
    }
    
    /**
     * 生成模拟Sentinel-3数据
     */
    private Map<String, Double> generateMockSentinel3Data(Double latitude, Double longitude) {
        Map<String, Double> s3Data = new HashMap<>();
        
        // 基于地理位置生成合理的光谱反射率值
        double baseReflectance = 0.04 + (latitude + 90) / 180 * 0.015;
        double longitudeEffect = Math.cos(Math.toRadians(longitude)) * 0.008;
        
        s3Data.put("Oa01", Math.max(0.005, baseReflectance + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa02", Math.max(0.005, baseReflectance * 0.95 + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa03", Math.max(0.005, baseReflectance * 0.9 + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa04", Math.max(0.005, baseReflectance * 0.85 + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa05", Math.max(0.005, baseReflectance * 0.8 + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa06", Math.max(0.005, baseReflectance * 0.75 + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa07", Math.max(0.005, baseReflectance * 0.7 + longitudeEffect + Math.random() * 0.008));
        s3Data.put("Oa08", Math.max(0.005, baseReflectance * 0.65 + longitudeEffect + Math.random() * 0.008));
        
        return s3Data;
    }
    
    /**
     * 计算叶绿素浓度神经网络预测值
     */
    private Double calculateChlNN(Map<String, Double> s2Data, Map<String, Double> s3Data) {
        try {
            // 简化的叶绿素浓度计算（基于NDCI等指数）
            double b4 = s2Data.getOrDefault("B4", 0.03); // 红光
            double b5 = s2Data.getOrDefault("B5", 0.025); // 红边
            double oa04 = s3Data.getOrDefault("Oa04", 0.03);
            double oa06 = s3Data.getOrDefault("Oa06", 0.02);
            
            // NDCI (Normalized Difference Chlorophyll Index)
            double ndci = (b5 - b4) / (b5 + b4);
            
            // 基于经验公式计算叶绿素浓度
            double chl = Math.max(0.1, Math.min(10.0, 2.0 + ndci * 5.0 + (oa04 - oa06) * 10.0));
            
            return Math.round(chl * 100.0) / 100.0; // 保留两位小数
            
        } catch (Exception e) {
            logger.warn("叶绿素浓度计算失败，使用默认值: {}", e.getMessage());
            return 1.5; // 默认值
        }
    }
    
    /**
     * 计算总悬浮物浓度神经网络预测值
     */
    private Double calculateTsmNN(Map<String, Double> s2Data, Map<String, Double> s3Data) {
        try {
            // 简化的总悬浮物浓度计算
            double b4 = s2Data.getOrDefault("B4", 0.03); // 红光
            double b8 = s2Data.getOrDefault("B8", 0.01); // 近红外
            double oa08 = s3Data.getOrDefault("Oa08", 0.015);
            
            // 基于红光和近红外波段的经验公式
            double tsm = Math.max(0.5, Math.min(50.0, 10.0 * b4 + 15.0 * b8 + 20.0 * oa08));
            
            return Math.round(tsm * 100.0) / 100.0; // 保留两位小数
            
        } catch (Exception e) {
            logger.warn("总悬浮物浓度计算失败，使用默认值: {}", e.getMessage());
            return 2.0; // 默认值
        }
    }
    
    /**
     * 检查API配置是否缺失
     */
    private boolean isApiConfigurationMissing() {
        return (sentinel2ApiUrl == null || sentinel2ApiUrl.isEmpty()) &&
               (sentinel3ApiUrl == null || sentinel3ApiUrl.isEmpty()) ||
               (apiKey == null || apiKey.isEmpty());
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(Double latitude, Double longitude, LocalDateTime dateTime) {
        return String.format("sat_data_%f_%f_%s", 
            latitude, longitude, 
            dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
    
    /**
     * 获取缓存数据
     */
    private SatelliteDataResponse getCachedData(String cacheKey) {
        SatelliteDataResponse cached = cache.get(cacheKey);
        if (cached != null) {
            // 检查缓存是否过期
            long cacheTime = cached.getTimestamp() != null ? 
                cached.getTimestamp().getTime() : System.currentTimeMillis();
            if (System.currentTimeMillis() - cacheTime < CACHE_EXPIRY_MS) {
                return cached;
            } else {
                cache.remove(cacheKey);
            }
        }
        return null;
    }
    
    /**
     * 缓存数据
     */
    private void cacheData(String cacheKey, SatelliteDataResponse data) {
        data.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        cache.put(cacheKey, data);
        
        // 清理过期缓存
        cleanupExpiredCache();
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            SatelliteDataResponse data = entry.getValue();
            long cacheTime = data.getTimestamp() != null ? 
                data.getTimestamp().getTime() : currentTime;
            return currentTime - cacheTime >= CACHE_EXPIRY_MS;
        });
    }
    
    /**
     * 安全获取Double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("无法解析数值: {} = {}", key, value);
                return 0.0;
            }
        }
        return 0.0;
    }
}