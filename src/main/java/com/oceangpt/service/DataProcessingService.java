package com.oceangpt.service;

import com.oceangpt.model.OceanData;
import com.oceangpt.model.PredictionResult;
import com.oceangpt.repository.OceanDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

/**
 * 数据处理服务类
 * 负责数据的高级处理、分析和特征工程
 */
@Service
public class DataProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingService.class);
    
    @Autowired
    private OceanDataRepository oceanDataRepository;
    
    @Value("${oceangpt.data.csv-path:D:/sentinel-2 reflectance}")
    private String csvBasePath;
    
    /**
     * 从CSV文件加载历史数据
     * @param latitude 纬度
     * @param longitude 经度
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 海洋数据列表
     */
    public List<OceanData> loadHistoricalDataFromCsv(double latitude, double longitude, 
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("从CSV文件加载历史数据 - 位置: ({}, {}), 时间范围: {} 到 {}", 
                   latitude, longitude, startTime, endTime);
        
        List<OceanData> oceanDataList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        try {
            // 根据时间范围确定需要读取的CSV文件
            List<String> csvFiles = getCsvFilesForTimeRange(startTime, endTime);
            
            for (String csvFile : csvFiles) {
                String filePath = csvBasePath + "/" + csvFile;
                logger.debug("读取CSV文件: {}", filePath);
                
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    boolean isFirstLine = true;
                    
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue; // 跳过标题行
                        }
                        
                        try {
                            OceanData oceanData = parseCsvLine(line, formatter);
                            if (oceanData != null && isWithinTimeRange(oceanData.getTimestamp(), startTime, endTime) &&
                                isWithinLocationRange(oceanData.getLatitude(), oceanData.getLongitude(), latitude, longitude)) {
                                oceanDataList.add(oceanData);
                            }
                        } catch (Exception e) {
                            logger.warn("解析CSV行失败: {}, 错误: {}", line, e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    logger.warn("读取CSV文件失败: {}, 错误: {}", filePath, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("加载CSV数据时发生异常: {}", e.getMessage(), e);
        }
        
        logger.info("成功加载 {} 条历史数据", oceanDataList.size());
        return oceanDataList;
    }
    
    /**
     * 数据预处理
     * @param rawData 原始数据列表
     * @return 预处理后的数据
     */
    public List<OceanData> preprocessData(List<OceanData> rawData) {
        logger.info("开始数据预处理 - 原始数据量: {}", rawData.size());
        
        List<OceanData> processedData = new ArrayList<>();
        
        for (OceanData data : rawData) {
            try {
                // 数据清洗
                OceanData cleanedData = cleanData(data);
                
                // 数据验证
                if (validateData(cleanedData)) {
                    // 数据标准化
                    OceanData normalizedData = normalizeData(cleanedData);
                    processedData.add(normalizedData);
                } else {
                    logger.debug("数据验证失败，跳过数据点: {}", data);
                }
                
            } catch (Exception e) {
                logger.warn("数据预处理失败，跳过数据点: {}, 错误: {}", data, e.getMessage());
            }
        }
        
        logger.info("数据预处理完成 - 有效数据量: {}", processedData.size());
        return processedData;
    }
    
    /**
     * 时间序列数据聚合
     * @param data 原始数据列表
     * @param intervalHours 聚合时间间隔（小时）
     * @return 聚合后的数据
     */
    @Cacheable(value = "aggregatedData", key = "#data.size() + '_' + #intervalHours")
    public List<OceanData> aggregateTimeSeriesData(List<OceanData> data, int intervalHours) {
        logger.info("开始时间序列数据聚合 - 原始数据量: {}, 聚合间隔: {}小时", data.size(), intervalHours);
        
        if (data.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按时间间隔分组
        Map<LocalDateTime, List<OceanData>> groupedData = data.stream()
            .collect(Collectors.groupingBy(oceanData -> 
                truncateToInterval(oceanData.getTimestamp(), intervalHours)));
        
        List<OceanData> aggregatedData = new ArrayList<>();
        
        for (Map.Entry<LocalDateTime, List<OceanData>> entry : groupedData.entrySet()) {
            LocalDateTime intervalTime = entry.getKey();
            List<OceanData> intervalData = entry.getValue();
            
            OceanData aggregated = aggregateDataPoints(intervalData, intervalTime);
            aggregatedData.add(aggregated);
        }
        
        // 按时间排序
        aggregatedData.sort(Comparator.comparing(OceanData::getTimestamp));
        
        logger.info("时间序列聚合完成 - 聚合后数据量: {}", aggregatedData.size());
        return aggregatedData;
    }
    
    /**
     * 空间数据插值
     * @param knownPoints 已知数据点
     * @param targetLatitude 目标纬度
     * @param targetLongitude 目标经度
     * @param maxDistance 最大插值距离（公里）
     * @return 插值结果
     */
    public OceanData spatialInterpolation(List<OceanData> knownPoints, 
                                         double targetLatitude, double targetLongitude, 
                                         double maxDistance) {
        logger.info("执行空间插值 - 目标位置: ({}, {}), 已知点数量: {}", 
                   targetLatitude, targetLongitude, knownPoints.size());
        
        if (knownPoints.isEmpty()) {
            return null;
        }
        
        // 计算距离权重
        List<WeightedPoint> weightedPoints = new ArrayList<>();
        double totalWeight = 0.0;
        
        for (OceanData point : knownPoints) {
            double distance = calculateDistance(targetLatitude, targetLongitude, 
                                              point.getLatitude(), point.getLongitude());
            
            if (distance <= maxDistance) {
                double weight = 1.0 / (distance + 0.1); // 避免除零
                weightedPoints.add(new WeightedPoint(point, weight));
                totalWeight += weight;
            }
        }
        
        if (weightedPoints.isEmpty()) {
            logger.warn("在最大距离 {}km 内未找到有效数据点", maxDistance);
            return null;
        }
        
        // 执行加权平均插值
        OceanData interpolatedData = new OceanData();
        interpolatedData.setLatitude(targetLatitude);
        interpolatedData.setLongitude(targetLongitude);
        interpolatedData.setTimestamp(LocalDateTime.now());
        interpolatedData.setDataSource("INTERPOLATED");
        
        double weightedTemp = 0.0, weightedSalinity = 0.0, weightedPh = 0.0;
        double weightedOxygen = 0.0, weightedChlorophyll = 0.0, weightedPollution = 0.0;
        
        for (WeightedPoint wp : weightedPoints) {
            double normalizedWeight = wp.weight / totalWeight;
            OceanData point = wp.data;
            
            if (point.getSeaSurfaceTemperature() != null) {
                weightedTemp += point.getSeaSurfaceTemperature() * normalizedWeight;
            }
            if (point.getSalinity() != null) {
                weightedSalinity += point.getSalinity() * normalizedWeight;
            }
            if (point.getPhLevel() != null) {
                weightedPh += point.getPhLevel() * normalizedWeight;
            }
            if (point.getDissolvedOxygen() != null) {
                weightedOxygen += point.getDissolvedOxygen() * normalizedWeight;
            }
            if (point.getChlorophyllConcentration() != null) {
                weightedChlorophyll += point.getChlorophyllConcentration() * normalizedWeight;
            }
            if (point.getPollutionIndex() != null) {
                weightedPollution += point.getPollutionIndex() * normalizedWeight;
            }
        }
        
        interpolatedData.setSeaSurfaceTemperature(weightedTemp);
        interpolatedData.setSalinity(weightedSalinity);
        interpolatedData.setPhLevel(weightedPh);
        interpolatedData.setDissolvedOxygen(weightedOxygen);
        interpolatedData.setChlorophyllConcentration(weightedChlorophyll);
        interpolatedData.setPollutionIndex(weightedPollution);
        
        logger.info("空间插值完成 - 使用 {} 个数据点", weightedPoints.size());
        return interpolatedData;
    }
    
    /**
     * 异常值检测
     * @param data 数据列表
     * @param parameter 检测参数（temperature, salinity, ph等）
     * @param threshold 异常阈值（标准差倍数）
     * @return 异常值列表
     */
    public List<OceanData> detectAnomalies(List<OceanData> data, String parameter, double threshold) {
        logger.info("开始异常值检测 - 参数: {}, 阈值: {}, 数据量: {}", parameter, threshold, data.size());
        
        List<Double> values = extractParameterValues(data, parameter);
        
        if (values.size() < 3) {
            logger.warn("数据量不足，无法进行异常值检测");
            return new ArrayList<>();
        }
        
        // 计算均值和标准差
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        // 检测异常值
        List<OceanData> anomalies = new ArrayList<>();
        
        for (int i = 0; i < data.size(); i++) {
            if (i < values.size()) {
                double value = values.get(i);
                double zScore = Math.abs(value - mean) / stdDev;
                
                if (zScore > threshold) {
                    anomalies.add(data.get(i));
                }
            }
        }
        
        logger.info("异常值检测完成 - 发现 {} 个异常值", anomalies.size());
        return anomalies;
    }
    
    /**
     * 数据质量评估
     * @param data 数据列表
     * @return 质量评估报告
     */
    public Map<String, Object> assessDataQuality(List<OceanData> data) {
        logger.info("开始数据质量评估 - 数据量: {}", data.size());
        
        Map<String, Object> qualityReport = new HashMap<>();
        
        if (data.isEmpty()) {
            qualityReport.put("status", "NO_DATA");
            return qualityReport;
        }
        
        // 完整性评估
        Map<String, Integer> completeness = assessCompleteness(data);
        qualityReport.put("completeness", completeness);
        
        // 一致性评估
        Map<String, Object> consistency = assessConsistency(data);
        qualityReport.put("consistency", consistency);
        
        // 准确性评估
        Map<String, Object> accuracy = assessAccuracy(data);
        qualityReport.put("accuracy", accuracy);
        
        // 时效性评估
        Map<String, Object> timeliness = assessTimeliness(data);
        qualityReport.put("timeliness", timeliness);
        
        // 计算总体质量分数
        double overallScore = calculateOverallQualityScore(completeness, consistency, accuracy, timeliness);
        qualityReport.put("overallScore", overallScore);
        qualityReport.put("qualityLevel", categorizeQualityLevel(overallScore));
        
        logger.info("数据质量评估完成 - 总体分数: {}", overallScore);
        return qualityReport;
    }
    
    /**
     * 特征工程
     * @param data 原始数据
     * @return 特征增强后的数据
     */
    public List<Map<String, Double>> extractFeatures(List<OceanData> data) {
        logger.info("开始特征工程 - 数据量: {}", data.size());
        
        List<Map<String, Double>> features = new ArrayList<>();
        
        for (int i = 0; i < data.size(); i++) {
            OceanData current = data.get(i);
            Map<String, Double> featureMap = new HashMap<>();
            
            // 基础特征
            addBasicFeatures(featureMap, current);
            
            // 时间特征
            addTemporalFeatures(featureMap, current);
            
            // 空间特征
            addSpatialFeatures(featureMap, current);
            
            // 统计特征（基于历史数据）
            if (i >= 2) {
                List<OceanData> historicalData = data.subList(Math.max(0, i - 5), i);
                addStatisticalFeatures(featureMap, current, historicalData);
            }
            
            // 衍生特征
            addDerivedFeatures(featureMap, current);
            
            features.add(featureMap);
        }
        
        logger.info("特征工程完成 - 特征维度: {}", features.isEmpty() ? 0 : features.get(0).size());
        return features;
    }
    
    /**
     * 数据趋势分析
     * @param data 时间序列数据
     * @param parameter 分析参数
     * @return 趋势分析结果
     */
    public Map<String, Object> analyzeTrend(List<OceanData> data, String parameter) {
        logger.info("开始趋势分析 - 参数: {}, 数据量: {}", parameter, data.size());
        
        Map<String, Object> trendAnalysis = new HashMap<>();
        
        if (data.size() < 3) {
            trendAnalysis.put("status", "INSUFFICIENT_DATA");
            return trendAnalysis;
        }
        
        List<Double> values = extractParameterValues(data, parameter);
        List<Long> timestamps = data.stream()
            .map(d -> d.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .collect(Collectors.toList());
        
        // 线性回归分析趋势
        double[] trendCoefficients = calculateLinearRegression(timestamps, values);
        double slope = trendCoefficients[1];
        double intercept = trendCoefficients[0];
        
        // 趋势方向
        String trendDirection;
        if (Math.abs(slope) < 0.001) {
            trendDirection = "STABLE";
        } else if (slope > 0) {
            trendDirection = "INCREASING";
        } else {
            trendDirection = "DECREASING";
        }
        
        // 趋势强度
        double correlation = calculateCorrelation(timestamps, values);
        String trendStrength;
        if (Math.abs(correlation) > 0.8) {
            trendStrength = "STRONG";
        } else if (Math.abs(correlation) > 0.5) {
            trendStrength = "MODERATE";
        } else {
            trendStrength = "WEAK";
        }
        
        // 变化率
        double changeRate = slope * 3600 * 24; // 每天的变化率
        
        trendAnalysis.put("direction", trendDirection);
        trendAnalysis.put("strength", trendStrength);
        trendAnalysis.put("slope", slope);
        trendAnalysis.put("intercept", intercept);
        trendAnalysis.put("correlation", correlation);
        trendAnalysis.put("dailyChangeRate", changeRate);
        trendAnalysis.put("dataPoints", values.size());
        
        logger.info("趋势分析完成 - 方向: {}, 强度: {}", trendDirection, trendStrength);
        return trendAnalysis;
    }
    
    // ========== CSV数据处理辅助方法 ==========
    
    /**
     * 根据时间范围获取需要读取的CSV文件列表
     */
    private List<String> getCsvFilesForTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<String> csvFiles = new ArrayList<>();
        
        LocalDateTime current = startTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = endTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        while (!current.isAfter(end)) {
            String fileName = String.format("Sentinel2_Reflectance_bohai%d-%02d.csv", 
                                          current.getYear(), current.getMonthValue());
            csvFiles.add(fileName);
            current = current.plusMonths(1);
        }
        
        return csvFiles;
    }
    
    /**
     * 解析CSV行数据
     */
    private OceanData parseCsvLine(String line, DateTimeFormatter formatter) {
        try {
            String[] parts = line.split(",");
            if (parts.length < 8) {
                return null;
            }
            
            OceanData oceanData = new OceanData();
            oceanData.setLatitude(Double.parseDouble(parts[0].trim()));
            oceanData.setLongitude(Double.parseDouble(parts[1].trim()));
            oceanData.setTimestamp(LocalDateTime.parse(parts[2].trim(), formatter));
            oceanData.setSeaSurfaceTemperature(parseDoubleOrNull(parts[3]));
            oceanData.setSalinity(parseDoubleOrNull(parts[4]));
            oceanData.setPhLevel(parseDoubleOrNull(parts[5]));
            oceanData.setDissolvedOxygen(parseDoubleOrNull(parts[6]));
            oceanData.setChlorophyllConcentration(parseDoubleOrNull(parts[7]));
            
            if (parts.length > 8) {
                oceanData.setPollutionIndex(parseDoubleOrNull(parts[8]));
            }
            
            oceanData.setDataSource("S2/S3_CSV");
            return oceanData;
            
        } catch (Exception e) {
            logger.warn("解析CSV行失败: {}", line);
            return null;
        }
    }
    
    /**
     * 安全解析Double值
     */
    private Double parseDoubleOrNull(String value) {
        try {
            String trimmed = value.trim();
            if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || "N/A".equalsIgnoreCase(trimmed)) {
                return null;
            }
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 检查时间是否在范围内
     */
    private boolean isWithinTimeRange(LocalDateTime timestamp, LocalDateTime startTime, LocalDateTime endTime) {
        return !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime);
    }
    
    /**
     * 检查位置是否在范围内（简单的矩形范围检查）
     */
    private boolean isWithinLocationRange(double lat, double lon, double targetLat, double targetLon) {
        double latTolerance = 0.5; // 纬度容差
        double lonTolerance = 0.5; // 经度容差
        
        return Math.abs(lat - targetLat) <= latTolerance && Math.abs(lon - targetLon) <= lonTolerance;
    }
    
    /**
     * 数据清洗
     */
    private OceanData cleanData(OceanData data) {
        // 创建数据副本
        OceanData cleaned = new OceanData();
        cleaned.setLatitude(data.getLatitude());
        cleaned.setLongitude(data.getLongitude());
        cleaned.setTimestamp(data.getTimestamp());
        cleaned.setDataSource(data.getDataSource());
        
        // 清洗和修正数据值
        cleaned.setSeaSurfaceTemperature(cleanTemperature(data.getSeaSurfaceTemperature()));
        cleaned.setSalinity(cleanSalinity(data.getSalinity()));
        cleaned.setPhLevel(cleanPhLevel(data.getPhLevel()));
        cleaned.setDissolvedOxygen(cleanOxygen(data.getDissolvedOxygen()));
        cleaned.setChlorophyllConcentration(cleanChlorophyll(data.getChlorophyllConcentration()));
        cleaned.setPollutionIndex(cleanPollutionIndex(data.getPollutionIndex()));
        
        return cleaned;
    }
    
    /**
     * 数据验证
     */
    private boolean validateData(OceanData data) {
        // 基本验证
        if (data.getLatitude() < -90 || data.getLatitude() > 90) return false;
        if (data.getLongitude() < -180 || data.getLongitude() > 180) return false;
        if (data.getTimestamp() == null) return false;
        
        // 端对端模型只需要叶绿素浓度作为物理要素数据
        return data.getChlorophyllConcentration() != null;
    }
    
    /**
     * 数据标准化
     */
    private OceanData normalizeData(OceanData data) {
        // 这里可以实现数据标准化逻辑
        // 目前返回原数据
        return data;
    }
    
    // 数据清洗辅助方法
    private Double cleanTemperature(Double temp) {
        if (temp == null) return null;
        // 海水温度合理范围：-2°C 到 40°C
        return (temp >= -2.0 && temp <= 40.0) ? temp : null;
    }
    
    private Double cleanSalinity(Double salinity) {
        if (salinity == null) return null;
        // 海水盐度合理范围：0 到 50 PSU
        return (salinity >= 0.0 && salinity <= 50.0) ? salinity : null;
    }
    
    private Double cleanPhLevel(Double ph) {
        if (ph == null) return null;
        // pH值合理范围：6.0 到 9.0
        return (ph >= 6.0 && ph <= 9.0) ? ph : null;
    }
    
    private Double cleanOxygen(Double oxygen) {
        if (oxygen == null) return null;
        // 溶解氧合理范围：0 到 20 mg/L
        return (oxygen >= 0.0 && oxygen <= 20.0) ? oxygen : null;
    }
    
    private Double cleanChlorophyll(Double chlorophyll) {
        if (chlorophyll == null) return null;
        // 叶绿素浓度合理范围：0 到 100 mg/m³
        return (chlorophyll >= 0.0 && chlorophyll <= 100.0) ? chlorophyll : null;
    }
    
    private Double cleanPollutionIndex(Double pollution) {
        if (pollution == null) return null;
        // 污染指数合理范围：0 到 10
        return (pollution >= 0.0 && pollution <= 10.0) ? pollution : null;
    }
    
    /**
     * 数据相关性分析
     * @param data 数据列表
     * @return 相关性矩阵
     */
    public Map<String, Map<String, Double>> analyzeCorrelations(List<OceanData> data) {
        logger.info("开始相关性分析 - 数据量: {}", data.size());
        
        String[] parameters = {"temperature", "salinity", "ph", "oxygen", "chlorophyll", "pollution"};
        Map<String, Map<String, Double>> correlationMatrix = new HashMap<>();
        
        for (String param1 : parameters) {
            Map<String, Double> correlations = new HashMap<>();
            List<Double> values1 = extractParameterValues(data, param1);
            
            for (String param2 : parameters) {
                List<Double> values2 = extractParameterValues(data, param2);
                
                if (values1.size() == values2.size() && values1.size() > 1) {
                    double correlation = calculateCorrelation(values1, values2);
                    correlations.put(param2, correlation);
                } else {
                    correlations.put(param2, 0.0);
                }
            }
            
            correlationMatrix.put(param1, correlations);
        }
        
        logger.info("相关性分析完成");
        return correlationMatrix;
    }
    
    // 私有辅助方法
    
    private LocalDateTime truncateToInterval(LocalDateTime timestamp, int intervalHours) {
        long hours = timestamp.until(LocalDateTime.of(1970, 1, 1, 0, 0), ChronoUnit.HOURS);
        long intervalStart = (hours / intervalHours) * intervalHours;
        return LocalDateTime.of(1970, 1, 1, 0, 0).plusHours(intervalStart);
    }
    
    private OceanData aggregateDataPoints(List<OceanData> dataPoints, LocalDateTime intervalTime) {
        OceanData aggregated = new OceanData();
        aggregated.setTimestamp(intervalTime);
        aggregated.setDataSource("AGGREGATED");
        
        // 计算平均位置
        double avgLat = dataPoints.stream().mapToDouble(OceanData::getLatitude).average().orElse(0.0);
        double avgLon = dataPoints.stream().mapToDouble(OceanData::getLongitude).average().orElse(0.0);
        aggregated.setLatitude(avgLat);
        aggregated.setLongitude(avgLon);
        
        // 计算各参数的平均值
        OptionalDouble avgTemp = dataPoints.stream()
            .filter(d -> d.getSeaSurfaceTemperature() != null)
            .mapToDouble(OceanData::getSeaSurfaceTemperature)
            .average();
        if (avgTemp.isPresent()) {
            aggregated.setSeaSurfaceTemperature(avgTemp.getAsDouble());
        }
        
        OptionalDouble avgSalinity = dataPoints.stream()
            .filter(d -> d.getSalinity() != null)
            .mapToDouble(OceanData::getSalinity)
            .average();
        if (avgSalinity.isPresent()) {
            aggregated.setSalinity(avgSalinity.getAsDouble());
        }
        
        OptionalDouble avgPh = dataPoints.stream()
            .filter(d -> d.getPhLevel() != null)
            .mapToDouble(OceanData::getPhLevel)
            .average();
        if (avgPh.isPresent()) {
            aggregated.setPhLevel(avgPh.getAsDouble());
        }
        
        OptionalDouble avgOxygen = dataPoints.stream()
            .filter(d -> d.getDissolvedOxygen() != null)
            .mapToDouble(OceanData::getDissolvedOxygen)
            .average();
        if (avgOxygen.isPresent()) {
            aggregated.setDissolvedOxygen(avgOxygen.getAsDouble());
        }
        
        OptionalDouble avgChlorophyll = dataPoints.stream()
            .filter(d -> d.getChlorophyllConcentration() != null)
            .mapToDouble(OceanData::getChlorophyllConcentration)
            .average();
        if (avgChlorophyll.isPresent()) {
            aggregated.setChlorophyllConcentration(avgChlorophyll.getAsDouble());
        }
        
        OptionalDouble avgPollution = dataPoints.stream()
            .filter(d -> d.getPollutionIndex() != null)
            .mapToDouble(OceanData::getPollutionIndex)
            .average();
        if (avgPollution.isPresent()) {
            aggregated.setPollutionIndex(avgPollution.getAsDouble());
        }
        
        return aggregated;
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private List<Double> extractParameterValues(List<OceanData> data, String parameter) {
        List<Double> values = new ArrayList<>();
        
        for (OceanData oceanData : data) {
            Double value = null;
            switch (parameter.toLowerCase()) {
                case "temperature":
                    value = oceanData.getSeaSurfaceTemperature();
                    break;
                case "salinity":
                    value = oceanData.getSalinity();
                    break;
                case "ph":
                    value = oceanData.getPhLevel();
                    break;
                case "oxygen":
                    value = oceanData.getDissolvedOxygen();
                    break;
                case "chlorophyll":
                    value = oceanData.getChlorophyllConcentration();
                    break;
                case "pollution":
                    value = oceanData.getPollutionIndex();
                    break;
            }
            
            if (value != null) {
                values.add(value);
            }
        }
        
        return values;
    }
    
    private Map<String, Integer> assessCompleteness(List<OceanData> data) {
        Map<String, Integer> completeness = new HashMap<>();
        int total = data.size();
        
        long tempCount = data.stream().filter(d -> d.getSeaSurfaceTemperature() != null).count();
        long salinityCount = data.stream().filter(d -> d.getSalinity() != null).count();
        long phCount = data.stream().filter(d -> d.getPhLevel() != null).count();
        long oxygenCount = data.stream().filter(d -> d.getDissolvedOxygen() != null).count();
        long chlorophyllCount = data.stream().filter(d -> d.getChlorophyllConcentration() != null).count();
        
        completeness.put("temperature", (int) ((tempCount * 100) / total));
        completeness.put("salinity", (int) ((salinityCount * 100) / total));
        completeness.put("ph", (int) ((phCount * 100) / total));
        completeness.put("oxygen", (int) ((oxygenCount * 100) / total));
        completeness.put("chlorophyll", (int) ((chlorophyllCount * 100) / total));
        
        return completeness;
    }
    
    private Map<String, Object> assessConsistency(List<OceanData> data) {
        Map<String, Object> consistency = new HashMap<>();
        
        // 检查数据源一致性
        Set<String> dataSources = data.stream()
            .map(OceanData::getDataSource)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        consistency.put("dataSourceCount", dataSources.size());
        consistency.put("dataSources", dataSources);
        
        // 检查时间间隔一致性
        if (data.size() > 1) {
            List<Long> intervals = new ArrayList<>();
            for (int i = 1; i < data.size(); i++) {
                long interval = ChronoUnit.MINUTES.between(
                    data.get(i-1).getTimestamp(), data.get(i).getTimestamp());
                intervals.add(Math.abs(interval));
            }
            
            double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double intervalVariance = intervals.stream()
                .mapToDouble(i -> Math.pow(i - avgInterval, 2))
                .average().orElse(0.0);
            
            consistency.put("averageIntervalMinutes", avgInterval);
            consistency.put("intervalVariance", intervalVariance);
        }
        
        return consistency;
    }
    
    private Map<String, Object> assessAccuracy(List<OceanData> data) {
        Map<String, Object> accuracy = new HashMap<>();
        
        // 检查数值范围合理性
        long validTemp = data.stream()
            .filter(d -> d.getSeaSurfaceTemperature() != null)
            .filter(d -> d.getSeaSurfaceTemperature() >= -2.0 && d.getSeaSurfaceTemperature() <= 40.0)
            .count();
        
        long validSalinity = data.stream()
            .filter(d -> d.getSalinity() != null)
            .filter(d -> d.getSalinity() >= 25.0 && d.getSalinity() <= 45.0)
            .count();
        
        long validPh = data.stream()
            .filter(d -> d.getPhLevel() != null)
            .filter(d -> d.getPhLevel() >= 7.0 && d.getPhLevel() <= 9.0)
            .count();
        
        long totalTemp = data.stream().filter(d -> d.getSeaSurfaceTemperature() != null).count();
        long totalSalinity = data.stream().filter(d -> d.getSalinity() != null).count();
        long totalPh = data.stream().filter(d -> d.getPhLevel() != null).count();
        
        accuracy.put("temperatureAccuracy", totalTemp > 0 ? (validTemp * 100.0 / totalTemp) : 100.0);
        accuracy.put("salinityAccuracy", totalSalinity > 0 ? (validSalinity * 100.0 / totalSalinity) : 100.0);
        accuracy.put("phAccuracy", totalPh > 0 ? (validPh * 100.0 / totalPh) : 100.0);
        
        return accuracy;
    }
    
    private Map<String, Object> assessTimeliness(List<OceanData> data) {
        Map<String, Object> timeliness = new HashMap<>();
        
        if (!data.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime latest = data.stream()
                .map(OceanData::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(now);
            
            long hoursOld = ChronoUnit.HOURS.between(latest, now);
            timeliness.put("latestDataAgeHours", hoursOld);
            
            String freshnessLevel;
            if (hoursOld <= 1) {
                freshnessLevel = "VERY_FRESH";
            } else if (hoursOld <= 6) {
                freshnessLevel = "FRESH";
            } else if (hoursOld <= 24) {
                freshnessLevel = "ACCEPTABLE";
            } else {
                freshnessLevel = "STALE";
            }
            
            timeliness.put("freshnessLevel", freshnessLevel);
        }
        
        return timeliness;
    }
    
    private double calculateOverallQualityScore(Map<String, Integer> completeness,
                                               Map<String, Object> consistency,
                                               Map<String, Object> accuracy,
                                               Map<String, Object> timeliness) {
        // 计算完整性分数
        double completenessScore = completeness.values().stream()
            .mapToInt(Integer::intValue)
            .average().orElse(0.0);
        
        // 计算准确性分数
        double accuracyScore = 0.0;
        if (accuracy.containsKey("temperatureAccuracy")) {
            accuracyScore = ((Double) accuracy.get("temperatureAccuracy") +
                           (Double) accuracy.get("salinityAccuracy") +
                           (Double) accuracy.get("phAccuracy")) / 3.0;
        }
        
        // 计算时效性分数
        double timelinessScore = 100.0;
        if (timeliness.containsKey("latestDataAgeHours")) {
            long ageHours = (Long) timeliness.get("latestDataAgeHours");
            timelinessScore = Math.max(0, 100 - (ageHours * 2)); // 每小时减2分
        }
        
        // 加权平均
        return (completenessScore * 0.4 + accuracyScore * 0.4 + timelinessScore * 0.2);
    }
    
    private String categorizeQualityLevel(double score) {
        if (score >= 90) {
            return "EXCELLENT";
        } else if (score >= 80) {
            return "GOOD";
        } else if (score >= 70) {
            return "FAIR";
        } else if (score >= 60) {
            return "POOR";
        } else {
            return "VERY_POOR";
        }
    }
    
    private void addBasicFeatures(Map<String, Double> features, OceanData data) {
        features.put("latitude", data.getLatitude());
        features.put("longitude", data.getLongitude());
        features.put("temperature", data.getSeaSurfaceTemperature() != null ? data.getSeaSurfaceTemperature() : 15.0);
        features.put("salinity", data.getSalinity() != null ? data.getSalinity() : 35.0);
        features.put("ph", data.getPhLevel() != null ? data.getPhLevel() : 8.1);
        features.put("oxygen", data.getDissolvedOxygen() != null ? data.getDissolvedOxygen() : 8.0);
        features.put("chlorophyll", data.getChlorophyllConcentration() != null ? data.getChlorophyllConcentration() : 0.5);
    }
    
    private void addTemporalFeatures(Map<String, Double> features, OceanData data) {
        LocalDateTime timestamp = data.getTimestamp();
        features.put("hour", (double) timestamp.getHour());
        features.put("dayOfWeek", (double) timestamp.getDayOfWeek().getValue());
        features.put("dayOfMonth", (double) timestamp.getDayOfMonth());
        features.put("month", (double) timestamp.getMonthValue());
        features.put("season", (double) ((timestamp.getMonthValue() - 1) / 3 + 1));
    }
    
    private void addSpatialFeatures(Map<String, Double> features, OceanData data) {
        double lat = data.getLatitude();
        double lon = data.getLongitude();
        
        // 距离赤道的距离
        features.put("distanceFromEquator", Math.abs(lat));
        
        // 海洋区域分类（简化）
        if (lat > 60) {
            features.put("oceanRegion", 1.0); // 极地
        } else if (lat > 30) {
            features.put("oceanRegion", 2.0); // 温带
        } else if (lat > -30) {
            features.put("oceanRegion", 3.0); // 热带
        } else {
            features.put("oceanRegion", 4.0); // 南半球
        }
    }
    
    private void addStatisticalFeatures(Map<String, Double> features, OceanData current, List<OceanData> historical) {
        if (historical.isEmpty()) return;
        
        // 温度变化趋势
        List<Double> temps = historical.stream()
            .filter(d -> d.getSeaSurfaceTemperature() != null)
            .map(OceanData::getSeaSurfaceTemperature)
            .collect(Collectors.toList());
        
        if (!temps.isEmpty()) {
            double avgTemp = temps.stream().mapToDouble(Double::doubleValue).average().orElse(15.0);
            double currentTemp = current.getSeaSurfaceTemperature() != null ? current.getSeaSurfaceTemperature() : 15.0;
            features.put("tempTrend", currentTemp - avgTemp);
            features.put("tempVariability", calculateStandardDeviation(temps));
        }
    }
    
    private void addDerivedFeatures(Map<String, Double> features, OceanData data) {
        // 密度指数（基于温度和盐度）
        double temp = features.get("temperature");
        double salinity = features.get("salinity");
        double densityIndex = salinity / (temp + 10); // 简化的密度指数
        features.put("densityIndex", densityIndex);
        
        // 富营养化指数
        double chlorophyll = features.get("chlorophyll");
        double oxygen = features.get("oxygen");
        double eutrophicationIndex = chlorophyll * 2 - (oxygen - 6);
        features.put("eutrophicationIndex", Math.max(0, eutrophicationIndex));
    }
    
    private double[] calculateLinearRegression(List<Long> x, List<Double> y) {
        int n = Math.min(x.size(), y.size());
        if (n < 2) return new double[]{0, 0};
        
        double sumX = x.stream().mapToLong(Long::longValue).sum();
        double sumY = y.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0, sumXX = 0;
        
        for (int i = 0; i < n; i++) {
            sumXY += x.get(i) * y.get(i);
            sumXX += x.get(i) * x.get(i);
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        return new double[]{intercept, slope};
    }
    
    private double calculateCorrelation(List<? extends Number> x, List<? extends Number> y) {
        int n = Math.min(x.size(), y.size());
        if (n < 2) return 0.0;
        
        double sumX = x.stream().mapToDouble(Number::doubleValue).sum();
        double sumY = y.stream().mapToDouble(Number::doubleValue).sum();
        double sumXY = 0, sumXX = 0, sumYY = 0;
        
        for (int i = 0; i < n; i++) {
            double xi = x.get(i).doubleValue();
            double yi = y.get(i).doubleValue();
            sumXY += xi * yi;
            sumXX += xi * xi;
            sumYY += yi * yi;
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumXX - sumX * sumX) * (n * sumYY - sumY * sumY));
        
        return denominator != 0 ? numerator / denominator : 0.0;
    }
    
    private double calculateStandardDeviation(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    // 内部类
    private static class WeightedPoint {
        final OceanData data;
        final double weight;
        
        WeightedPoint(OceanData data, double weight) {
            this.data = data;
            this.weight = weight;
        }
    }
}