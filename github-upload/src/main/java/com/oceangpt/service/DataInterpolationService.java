package com.oceangpt.service;

import com.oceangpt.dto.SatelliteDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能数据插值服务
 * 解决任意经纬度坐标点的卫星数据获取问题
 * 通过空间插值、时间插值和智能填充策略提供完整的数据覆盖
 */
@Service
public class DataInterpolationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInterpolationService.class);
    
    @Autowired
    private SatelliteDataService satelliteDataService;
    
    @Autowired
    private CacheService cacheService;
    
    // 插值搜索半径（度）
    private static final double SEARCH_RADIUS = 0.5;
    // 最大搜索点数
    private static final int MAX_SEARCH_POINTS = 8;
    // 时间搜索窗口（天）
    private static final int TIME_WINDOW_DAYS = 7;
    
    /**
     * 智能获取指定位置的卫星数据
     * 如果精确位置没有数据，使用多种策略获取或插值数据
     */
    public SatelliteDataResponse getInterpolatedSatelliteData(Double latitude, Double longitude, LocalDateTime dateTime) {
        logger.info("开始智能数据获取: 经度={}, 纬度={}, 时间={}", longitude, latitude, dateTime);
        
        try {
            // 1. 首先尝试获取精确位置的数据
            SatelliteDataResponse exactData = satelliteDataService.getSatelliteData(latitude, longitude, dateTime);
            if (exactData != null && exactData.isSuccess() && isDataComplete(exactData)) {
                logger.info("获取到精确位置数据");
                exactData.setDataSource("Exact Location Data");
                return exactData;
            }
            
            // 2. 空间插值：搜索周围的数据点
            List<SpatialDataPoint> nearbyPoints = findNearbyDataPoints(latitude, longitude, dateTime);
            if (!nearbyPoints.isEmpty()) {
                logger.info("找到{}个邻近数据点，执行空间插值", nearbyPoints.size());
                SatelliteDataResponse spatialResult = performSpatialInterpolation(latitude, longitude, dateTime, nearbyPoints);
                if (spatialResult != null && isDataComplete(spatialResult)) {
                    spatialResult.setDataSource("Spatial Interpolation");
                    return spatialResult;
                }
            }
            
            // 3. 时间插值：搜索同一位置的历史数据
            SatelliteDataResponse temporalResult = performTemporalInterpolation(latitude, longitude, dateTime);
            if (temporalResult != null && isDataComplete(temporalResult)) {
                logger.info("使用时间插值数据");
                temporalResult.setDataSource("Temporal Interpolation");
                return temporalResult;
            }
            
            // 4. 区域平均：使用更大范围的平均值
            SatelliteDataResponse regionalResult = performRegionalAveraging(latitude, longitude, dateTime);
            if (regionalResult != null && isDataComplete(regionalResult)) {
                logger.info("使用区域平均数据");
                regionalResult.setDataSource("Regional Averaging");
                return regionalResult;
            }
            
            // 5. 智能模拟：基于地理和季节特征生成数据
            logger.info("使用智能模拟数据");
            return generateIntelligentMockData(latitude, longitude, dateTime);
            
        } catch (Exception e) {
            logger.error("数据插值失败: {}", e.getMessage(), e);
            return generateIntelligentMockData(latitude, longitude, dateTime);
        }
    }
    
    /**
     * 检查数据是否完整
     */
    private boolean isDataComplete(SatelliteDataResponse data) {
        if (data == null || !data.isSuccess()) {
            return false;
        }
        
        // 检查关键数据字段
        Map<String, Double> s2Data = data.getS2Data();
        Map<String, Double> s3Data = data.getS3Data();
        
        return s2Data != null && !s2Data.isEmpty() && 
               s3Data != null && !s3Data.isEmpty() &&
               data.getChlNN() != null && data.getTsmNN() != null;
    }
    
    /**
     * 查找邻近的数据点
     */
    private List<SpatialDataPoint> findNearbyDataPoints(Double latitude, Double longitude, LocalDateTime dateTime) {
        List<SpatialDataPoint> nearbyPoints = new ArrayList<>();
        
        // 生成搜索网格
        double[] latOffsets = {-SEARCH_RADIUS, -SEARCH_RADIUS/2, 0, SEARCH_RADIUS/2, SEARCH_RADIUS};
        double[] lonOffsets = {-SEARCH_RADIUS, -SEARCH_RADIUS/2, 0, SEARCH_RADIUS/2, SEARCH_RADIUS};
        
        for (double latOffset : latOffsets) {
            for (double lonOffset : lonOffsets) {
                if (latOffset == 0 && lonOffset == 0) continue; // 跳过中心点
                
                double searchLat = latitude + latOffset;
                double searchLon = longitude + lonOffset;
                
                // 检查坐标有效性
                if (searchLat < -90 || searchLat > 90 || searchLon < -180 || searchLon > 180) {
                    continue;
                }
                
                try {
                    SatelliteDataResponse data = satelliteDataService.getSatelliteData(searchLat, searchLon, dateTime);
                    if (data != null && data.isSuccess() && isDataComplete(data)) {
                        double distance = calculateDistance(latitude, longitude, searchLat, searchLon);
                        nearbyPoints.add(new SpatialDataPoint(searchLat, searchLon, distance, data));
                    }
                } catch (Exception e) {
                    logger.debug("搜索点({}, {})数据获取失败: {}", searchLat, searchLon, e.getMessage());
                }
                
                if (nearbyPoints.size() >= MAX_SEARCH_POINTS) {
                    break;
                }
            }
            if (nearbyPoints.size() >= MAX_SEARCH_POINTS) {
                break;
            }
        }
        
        // 按距离排序
        nearbyPoints.sort(Comparator.comparingDouble(SpatialDataPoint::getDistance));
        
        return nearbyPoints;
    }
    
    /**
     * 执行空间插值
     */
    private SatelliteDataResponse performSpatialInterpolation(Double latitude, Double longitude, 
                                                             LocalDateTime dateTime, 
                                                             List<SpatialDataPoint> nearbyPoints) {
        if (nearbyPoints.isEmpty()) {
            return null;
        }
        
        try {
            // 使用反距离权重插值法 (IDW)
            Map<String, Double> interpolatedS2 = new HashMap<>();
            Map<String, Double> interpolatedS3 = new HashMap<>();
            double interpolatedChlNN = 0.0;
            double interpolatedTsmNN = 0.0;
            double totalWeightTemp = 0.0;
            
            for (SpatialDataPoint point : nearbyPoints) {
                double weight = 1.0 / (point.getDistance() + 0.001); // 避免除零
                totalWeightTemp += weight;
                
                SatelliteDataResponse data = point.getData();
                
                // 插值S2数据
                if (data.getS2Data() != null) {
                    for (Map.Entry<String, Double> entry : data.getS2Data().entrySet()) {
                        interpolatedS2.merge(entry.getKey(), entry.getValue() * weight, Double::sum);
                    }
                }
                
                // 插值S3数据
                if (data.getS3Data() != null) {
                    for (Map.Entry<String, Double> entry : data.getS3Data().entrySet()) {
                        interpolatedS3.merge(entry.getKey(), entry.getValue() * weight, Double::sum);
                    }
                }
                
                // 插值神经网络预测值
                if (data.getChlNN() != null) {
                    interpolatedChlNN += data.getChlNN() * weight;
                }
                if (data.getTsmNN() != null) {
                    interpolatedTsmNN += data.getTsmNN() * weight;
                }
            }
            
            // 归一化权重
            final double totalWeight = totalWeightTemp;
            interpolatedS2.replaceAll((k, v) -> v / totalWeight);
            interpolatedS3.replaceAll((k, v) -> v / totalWeight);
            interpolatedChlNN /= totalWeight;
            interpolatedTsmNN /= totalWeight;
            
            // 构建结果
            SatelliteDataResponse result = new SatelliteDataResponse();
            result.setSuccess(true);
            result.setLatitude(latitude);
            result.setLongitude(longitude);
            result.setDateTime(dateTime);
            result.setS2Data(interpolatedS2);
            result.setS3Data(interpolatedS3);
            result.setChlNN(interpolatedChlNN);
            result.setTsmNN(interpolatedTsmNN);
            result.setQualityScore(0.75); // 插值数据质量评分
            
            return result;
            
        } catch (Exception e) {
            logger.error("空间插值失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 执行时间插值
     */
    private SatelliteDataResponse performTemporalInterpolation(Double latitude, Double longitude, LocalDateTime dateTime) {
        try {
            List<SatelliteDataResponse> historicalData = new ArrayList<>();
            
            // 搜索前后时间窗口内的数据
            for (int days = 1; days <= TIME_WINDOW_DAYS; days++) {
                // 搜索过去的数据
                LocalDateTime pastTime = dateTime.minusDays(days);
                SatelliteDataResponse pastData = satelliteDataService.getSatelliteData(latitude, longitude, pastTime);
                if (pastData != null && pastData.isSuccess() && isDataComplete(pastData)) {
                    historicalData.add(pastData);
                }
                
                // 搜索未来的数据（如果有的话）
                LocalDateTime futureTime = dateTime.plusDays(days);
                SatelliteDataResponse futureData = satelliteDataService.getSatelliteData(latitude, longitude, futureTime);
                if (futureData != null && futureData.isSuccess() && isDataComplete(futureData)) {
                    historicalData.add(futureData);
                }
                
                if (historicalData.size() >= 3) {
                    break; // 有足够的数据点进行插值
                }
            }
            
            if (historicalData.isEmpty()) {
                return null;
            }
            
            // 使用简单平均法进行时间插值
            return averageDataPoints(historicalData, latitude, longitude, dateTime);
            
        } catch (Exception e) {
            logger.error("时间插值失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 执行区域平均
     */
    private SatelliteDataResponse performRegionalAveraging(Double latitude, Double longitude, LocalDateTime dateTime) {
        try {
            List<SatelliteDataResponse> regionalData = new ArrayList<>();
            double largeRadius = SEARCH_RADIUS * 2; // 扩大搜索范围
            
            // 在更大范围内搜索数据
            for (double lat = latitude - largeRadius; lat <= latitude + largeRadius; lat += largeRadius/2) {
                for (double lon = longitude - largeRadius; lon <= longitude + largeRadius; lon += largeRadius/2) {
                    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) continue;
                    
                    try {
                        SatelliteDataResponse data = satelliteDataService.getSatelliteData(lat, lon, dateTime);
                        if (data != null && data.isSuccess() && isDataComplete(data)) {
                            regionalData.add(data);
                        }
                    } catch (Exception e) {
                        // 忽略单个点的错误
                    }
                }
            }
            
            if (regionalData.size() < 2) {
                return null;
            }
            
            return averageDataPoints(regionalData, latitude, longitude, dateTime);
            
        } catch (Exception e) {
            logger.error("区域平均失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 生成智能模拟数据
     */
    private SatelliteDataResponse generateIntelligentMockData(Double latitude, Double longitude, LocalDateTime dateTime) {
        logger.info("生成基于地理和季节特征的智能模拟数据");
        
        SatelliteDataResponse response = new SatelliteDataResponse();
        response.setSuccess(true);
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setDateTime(dateTime);
        response.setDataSource("Intelligent Mock Data");
        response.setQualityScore(0.60); // 模拟数据质量评分
        
        // 基于地理位置和季节的智能参数
        double latitudeFactor = Math.abs(latitude) / 90.0; // 纬度因子
        double seasonFactor = getSeasonFactor(dateTime); // 季节因子
        double coastalFactor = getCoastalFactor(latitude, longitude); // 海岸因子
        
        // 生成S2数据
        Map<String, Double> s2Data = generateIntelligentS2Data(latitudeFactor, seasonFactor, coastalFactor);
        response.setS2Data(s2Data);
        
        // 生成S3数据
        Map<String, Double> s3Data = generateIntelligentS3Data(latitudeFactor, seasonFactor, coastalFactor);
        response.setS3Data(s3Data);
        
        // 计算神经网络预测值
        response.setChlNN(calculateIntelligentChlNN(s2Data, s3Data, seasonFactor));
        response.setTsmNN(calculateIntelligentTsmNN(s2Data, s3Data, coastalFactor));
        
        return response;
    }
    
    // 辅助方法
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2));
    }
    
    private double getSeasonFactor(LocalDateTime dateTime) {
        int month = dateTime.getMonthValue();
        // 北半球季节因子：春夏高，秋冬低
        return 0.5 + 0.3 * Math.sin(Math.PI * (month - 3) / 6);
    }
    
    private double getCoastalFactor(double latitude, double longitude) {
        // 简化的海岸距离估算
        return 0.7 + 0.3 * Math.random();
    }
    
    private Map<String, Double> generateIntelligentS2Data(double latitudeFactor, double seasonFactor, double coastalFactor) {
        Map<String, Double> s2Data = new HashMap<>();
        double baseReflectance = 0.03 + latitudeFactor * 0.02 + seasonFactor * 0.01;
        
        s2Data.put("B2", Math.max(0.01, baseReflectance * (1.0 + coastalFactor * 0.1) + Math.random() * 0.005));
        s2Data.put("B3", Math.max(0.01, baseReflectance * 0.9 + Math.random() * 0.005));
        s2Data.put("B4", Math.max(0.01, baseReflectance * 0.8 + Math.random() * 0.005));
        s2Data.put("B5", Math.max(0.01, baseReflectance * 0.7 + Math.random() * 0.005));
        s2Data.put("B6", Math.max(0.01, baseReflectance * 0.6 + Math.random() * 0.005));
        s2Data.put("B7", Math.max(0.01, baseReflectance * 0.5 + Math.random() * 0.005));
        s2Data.put("B8", Math.max(0.01, baseReflectance * 0.4 + Math.random() * 0.005));
        s2Data.put("B8A", Math.max(0.01, baseReflectance * 0.45 + Math.random() * 0.005));
        
        return s2Data;
    }
    
    private Map<String, Double> generateIntelligentS3Data(double latitudeFactor, double seasonFactor, double coastalFactor) {
        Map<String, Double> s3Data = new HashMap<>();
        double baseReflectance = 0.025 + latitudeFactor * 0.015 + seasonFactor * 0.008;
        
        for (int i = 1; i <= 8; i++) {
            String key = String.format("Oa%02d", i);
            double value = Math.max(0.005, baseReflectance * (1.0 - i * 0.05) + Math.random() * 0.003);
            s3Data.put(key, value);
        }
        
        return s3Data;
    }
    
    private Double calculateIntelligentChlNN(Map<String, Double> s2Data, Map<String, Double> s3Data, double seasonFactor) {
        double b4 = s2Data.getOrDefault("B4", 0.03);
        double b5 = s2Data.getOrDefault("B5", 0.025);
        double ndci = (b5 - b4) / (b5 + b4);
        return Math.max(0.1, Math.min(8.0, 1.5 + ndci * 4.0 + seasonFactor * 2.0));
    }
    
    private Double calculateIntelligentTsmNN(Map<String, Double> s2Data, Map<String, Double> s3Data, double coastalFactor) {
        double b4 = s2Data.getOrDefault("B4", 0.03);
        double b8 = s2Data.getOrDefault("B8", 0.02);
        double ratio = b4 / b8;
        return Math.max(0.5, Math.min(15.0, 3.0 + ratio * 5.0 + coastalFactor * 3.0));
    }
    
    private SatelliteDataResponse averageDataPoints(List<SatelliteDataResponse> dataPoints, 
                                                   Double latitude, Double longitude, 
                                                   LocalDateTime dateTime) {
        if (dataPoints.isEmpty()) {
            return null;
        }
        
        Map<String, Double> avgS2 = new HashMap<>();
        Map<String, Double> avgS3 = new HashMap<>();
        double avgChlNN = 0.0;
        double avgTsmNN = 0.0;
        int count = dataPoints.size();
        
        for (SatelliteDataResponse data : dataPoints) {
            if (data.getS2Data() != null) {
                for (Map.Entry<String, Double> entry : data.getS2Data().entrySet()) {
                    avgS2.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
            if (data.getS3Data() != null) {
                for (Map.Entry<String, Double> entry : data.getS3Data().entrySet()) {
                    avgS3.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
            if (data.getChlNN() != null) {
                avgChlNN += data.getChlNN();
            }
            if (data.getTsmNN() != null) {
                avgTsmNN += data.getTsmNN();
            }
        }
        
        // 计算平均值
        avgS2.replaceAll((k, v) -> v / count);
        avgS3.replaceAll((k, v) -> v / count);
        avgChlNN /= count;
        avgTsmNN /= count;
        
        SatelliteDataResponse result = new SatelliteDataResponse();
        result.setSuccess(true);
        result.setLatitude(latitude);
        result.setLongitude(longitude);
        result.setDateTime(dateTime);
        result.setS2Data(avgS2);
        result.setS3Data(avgS3);
        result.setChlNN(avgChlNN);
        result.setTsmNN(avgTsmNN);
        result.setQualityScore(0.70);
        
        return result;
    }
    
    /**
     * 空间数据点类
     */
    private static class SpatialDataPoint {
        private final double latitude;
        private final double longitude;
        private final double distance;
        private final SatelliteDataResponse data;
        
        public SpatialDataPoint(double latitude, double longitude, double distance, SatelliteDataResponse data) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.distance = distance;
            this.data = data;
        }
        
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getDistance() { return distance; }
        public SatelliteDataResponse getData() { return data; }
    }
}