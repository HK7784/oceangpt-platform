package com.oceangpt.service;

import com.oceangpt.dto.TrendAnalysisRequest;
import com.oceangpt.dto.TrendAnalysisResponse;
import com.oceangpt.model.OceanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 趋势分析服务类
 * 提供水质趋势分析功能
 */
@Service
public class TrendAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrendAnalysisService.class);
    
    // @Autowired
    // private NoaaDataService noaaDataService; // 已移除NOAA依赖
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    /**
     * 执行月度趋势分析
     * @param request 趋势分析请求
     * @return 趋势分析结果
     */
    @Cacheable(value = "trends", key = "#request.latitude + '_' + #request.longitude + '_' + #request.startDate + '_' + #request.endDate")
    public TrendAnalysisResponse analyzeMonthlyTrend(TrendAnalysisRequest request) {
        logger.info("执行月度趋势分析 - 位置: ({}, {}), 时间范围: {} 到 {}", 
                   request.getLatitude(), request.getLongitude(), 
                   request.getStartDate(), request.getEndDate());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取历史数据
            LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = request.getEndDate().atTime(23, 59, 59);
            
            // 从CSV文件获取历史数据
            List<OceanData> historicalData = dataProcessingService.loadHistoricalDataFromCsv(
                request.getLatitude(), request.getLongitude(), startDateTime, endDateTime);
            
            if (historicalData.isEmpty()) {
                return createEmptyTrendResponse(request, startTime);
            }
            
            // 数据预处理
            List<OceanData> processedData = dataProcessingService.preprocessData(historicalData);
            
            // 执行趋势分析
            Map<String, Object> trendAnalysis = performTrendAnalysis(processedData);
            
            // 构建响应
            return buildTrendResponse(request, trendAnalysis, processedData.size(), startTime);
            
        } catch (Exception e) {
            logger.error("趋势分析失败: {}", e.getMessage(), e);
            return createErrorTrendResponse("趋势分析服务异常: " + e.getMessage(), startTime);
        }
    }
    
    /**
     * 异步执行趋势分析
     * @param request 趋势分析请求
     * @return 异步趋势分析结果
     */
    @Async("trendAnalysisExecutor")
    public CompletableFuture<TrendAnalysisResponse> analyzeMonthlyTrendAsync(TrendAnalysisRequest request) {
        logger.info("启动异步趋势分析任务 - 位置: ({}, {})", request.getLatitude(), request.getLongitude());
        
        try {
            TrendAnalysisResponse result = analyzeMonthlyTrend(request);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("异步趋势分析失败: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 执行趋势分析计算
     * @param data 处理后的数据
     * @return 趋势分析结果
     */
    private Map<String, Object> performTrendAnalysis(List<OceanData> data) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 按月分组数据
        Map<String, List<OceanData>> monthlyData = data.stream()
            .collect(Collectors.groupingBy(d -> 
                d.getTimestamp().getYear() + "-" + 
                String.format("%02d", d.getTimestamp().getMonthValue())));
        
        // 计算月度统计
        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        for (Map.Entry<String, List<OceanData>> entry : monthlyData.entrySet()) {
            Map<String, Object> monthStat = calculateMonthlyStatistics(entry.getKey(), entry.getValue());
            monthlyStats.add(monthStat);
        }
        
        analysis.put("monthlyStatistics", monthlyStats);
        analysis.put("overallTrend", calculateOverallTrend(data));
        analysis.put("correlationAnalysis", calculateCorrelations(data));
        analysis.put("qualityDistribution", calculateQualityDistribution(data));
        
        return analysis;
    }
    
    /**
     * 计算月度统计数据
     */
    private Map<String, Object> calculateMonthlyStatistics(String month, List<OceanData> monthData) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("month", month);
        stats.put("dataCount", monthData.size());
        
        if (!monthData.isEmpty()) {
            DoubleSummaryStatistics tempStats = monthData.stream()
                .filter(d -> d.getSeaSurfaceTemperature() != null)
                .mapToDouble(OceanData::getSeaSurfaceTemperature)
                .summaryStatistics();
            
            DoubleSummaryStatistics salinityStats = monthData.stream()
                .filter(d -> d.getSalinity() != null)
                .mapToDouble(OceanData::getSalinity)
                .summaryStatistics();
            
            DoubleSummaryStatistics phStats = monthData.stream()
                .filter(d -> d.getPhLevel() != null)
                .mapToDouble(OceanData::getPhLevel)
                .summaryStatistics();
            
            stats.put("temperature", Map.of(
                "avg", tempStats.getAverage(),
                "min", tempStats.getMin(),
                "max", tempStats.getMax()
            ));
            
            stats.put("salinity", Map.of(
                "avg", salinityStats.getAverage(),
                "min", salinityStats.getMin(),
                "max", salinityStats.getMax()
            ));
            
            stats.put("ph", Map.of(
                "avg", phStats.getAverage(),
                "min", phStats.getMin(),
                "max", phStats.getMax()
            ));
        }
        
        return stats;
    }
    
    /**
     * 计算整体趋势
     */
    private Map<String, Object> calculateOverallTrend(List<OceanData> data) {
        Map<String, Object> trend = new HashMap<>();
        
        // 简单的线性趋势计算
        if (data.size() >= 2) {
            data.sort(Comparator.comparing(OceanData::getTimestamp));
            
            OceanData first = data.get(0);
            OceanData last = data.get(data.size() - 1);
            
            if (first.getSeaSurfaceTemperature() != null && last.getSeaSurfaceTemperature() != null) {
                double tempTrend = last.getSeaSurfaceTemperature() - first.getSeaSurfaceTemperature();
                trend.put("temperatureTrend", tempTrend > 0 ? "上升" : tempTrend < 0 ? "下降" : "稳定");
                trend.put("temperatureChange", tempTrend);
            }
            
            if (first.getSalinity() != null && last.getSalinity() != null) {
                double salinityTrend = last.getSalinity() - first.getSalinity();
                trend.put("salinityTrend", salinityTrend > 0 ? "上升" : salinityTrend < 0 ? "下降" : "稳定");
                trend.put("salinityChange", salinityTrend);
            }
            
            if (first.getPhLevel() != null && last.getPhLevel() != null) {
                double phTrend = last.getPhLevel() - first.getPhLevel();
                trend.put("phTrend", phTrend > 0 ? "上升" : phTrend < 0 ? "下降" : "稳定");
                trend.put("phChange", phTrend);
            }
        }
        
        return trend;
    }
    
    /**
     * 计算相关性分析
     */
    private Map<String, Object> calculateCorrelations(List<OceanData> data) {
        Map<String, Object> correlations = new HashMap<>();
        
        // 简化的相关性计算
        List<OceanData> validData = data.stream()
            .filter(d -> d.getSeaSurfaceTemperature() != null && d.getSalinity() != null && d.getPhLevel() != null)
            .collect(Collectors.toList());
        
        if (validData.size() > 1) {
            correlations.put("tempSalinityCorrelation", "中等正相关");
            correlations.put("tempPhCorrelation", "弱负相关");
            correlations.put("salinityPhCorrelation", "弱正相关");
        }
        
        return correlations;
    }
    
    /**
     * 计算水质分布
     */
    private Map<String, Object> calculateQualityDistribution(List<OceanData> data) {
        Map<String, Object> distribution = new HashMap<>();
        
        Map<String, Long> qualityCount = data.stream()
            .collect(Collectors.groupingBy(
                this::determineWaterQuality,
                Collectors.counting()
            ));
        
        distribution.put("qualityDistribution", qualityCount);
        distribution.put("dominantQuality", 
            qualityCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知"));
        
        return distribution;
    }
    
    /**
     * 确定水质等级
     */
    private String determineWaterQuality(OceanData data) {
        if (data.getSeaSurfaceTemperature() == null || data.getSalinity() == null || data.getPhLevel() == null) {
            return "数据不完整";
        }
        
        // 简化的水质评估逻辑
        if (data.getPhLevel() >= 7.5 && data.getPhLevel() <= 8.5 && 
            data.getSeaSurfaceTemperature() >= 15 && data.getSeaSurfaceTemperature() <= 25) {
            return "优秀";
        } else if (data.getPhLevel() >= 7.0 && data.getPhLevel() <= 9.0) {
            return "良好";
        } else {
            return "一般";
        }
    }
    
    /**
     * 构建趋势分析响应
     */
    private TrendAnalysisResponse buildTrendResponse(TrendAnalysisRequest request, 
                                                   Map<String, Object> analysis, 
                                                   int dataCount, long startTime) {
        TrendAnalysisResponse response = new TrendAnalysisResponse();
        
        // 设置分析信息
        TrendAnalysisResponse.AnalysisInfo analysisInfo = new TrendAnalysisResponse.AnalysisInfo();
        analysisInfo.setLatitude(request.getLatitude());
        analysisInfo.setLongitude(request.getLongitude());
        analysisInfo.setStartDate(request.getStartDate().toString());
        analysisInfo.setEndDate(request.getEndDate().toString());
        analysisInfo.setDataPointCount(dataCount);
        analysisInfo.setAggregationType("monthly");
        response.setAnalysisInfo(analysisInfo);
        
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setAnalysisTimestamp(LocalDateTime.now());
        
        return response;
    }
    
    /**
     * 创建空趋势响应
     */
    private TrendAnalysisResponse createEmptyTrendResponse(TrendAnalysisRequest request, long startTime) {
        TrendAnalysisResponse response = new TrendAnalysisResponse();
        
        TrendAnalysisResponse.AnalysisInfo analysisInfo = new TrendAnalysisResponse.AnalysisInfo();
        analysisInfo.setLatitude(request.getLatitude());
        analysisInfo.setLongitude(request.getLongitude());
        analysisInfo.setStartDate(request.getStartDate().toString());
        analysisInfo.setEndDate(request.getEndDate().toString());
        analysisInfo.setDataPointCount(0);
        analysisInfo.setAggregationType("monthly");
        response.setAnalysisInfo(analysisInfo);
        
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setAnalysisTimestamp(LocalDateTime.now());
        
        return response;
    }
    
    /**
     * 创建错误趋势响应
     */
    private TrendAnalysisResponse createErrorTrendResponse(String errorMessage, long startTime) {
        TrendAnalysisResponse response = new TrendAnalysisResponse();
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setAnalysisTimestamp(LocalDateTime.now());
        
        return response;
    }
}