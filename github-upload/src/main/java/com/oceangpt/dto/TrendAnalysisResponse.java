package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 趋势分析响应DTO
 * 用于返回月度趋势分析结果
 */
@Schema(description = "月度趋势分析响应结果")
public class TrendAnalysisResponse {
    
    @Schema(description = "分析时间段")
    @JsonProperty("analysisInfo")
    private AnalysisInfo analysisInfo;
    
    @Schema(description = "历史数据趋势")
    @JsonProperty("historicalTrends")
    private List<TrendDataPoint> historicalTrends;
    
    @Schema(description = "预测数据趋势")
    @JsonProperty("forecastTrends")
    private List<TrendDataPoint> forecastTrends;
    
    @Schema(description = "统计摘要")
    @JsonProperty("statisticalSummary")
    private StatisticalSummary statisticalSummary;
    
    @Schema(description = "趋势指标")
    @JsonProperty("trendIndicators")
    private Map<String, TrendIndicator> trendIndicators;
    
    @Schema(description = "分析时间戳")
    @JsonProperty("analysisTimestamp")
    private LocalDateTime analysisTimestamp;
    
    @Schema(description = "处理时间（毫秒）")
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;
    
    // 内部类：分析信息
    @Schema(description = "分析基本信息")
    public static class AnalysisInfo {
        @JsonProperty("latitude")
        private Double latitude;
        
        @JsonProperty("longitude")
        private Double longitude;
        
        @JsonProperty("startDate")
        private String startDate;
        
        @JsonProperty("endDate")
        private String endDate;
        
        @JsonProperty("aggregationType")
        private String aggregationType;
        
        @JsonProperty("dataPointCount")
        private Integer dataPointCount;
        
        // 构造函数和getter/setter
        public AnalysisInfo() {}
        
        public AnalysisInfo(Double latitude, Double longitude, String startDate, String endDate, 
                          String aggregationType, Integer dataPointCount) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.startDate = startDate;
            this.endDate = endDate;
            this.aggregationType = aggregationType;
            this.dataPointCount = dataPointCount;
        }
        
        // Getter和Setter方法
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getAggregationType() { return aggregationType; }
        public void setAggregationType(String aggregationType) { this.aggregationType = aggregationType; }
        public Integer getDataPointCount() { return dataPointCount; }
        public void setDataPointCount(Integer dataPointCount) { this.dataPointCount = dataPointCount; }
    }
    
    // 内部类：趋势数据点
    @Schema(description = "趋势数据点")
    public static class TrendDataPoint {
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("values")
        private Map<String, Double> values;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        public TrendDataPoint() {}
        
        public TrendDataPoint(String timestamp, Map<String, Double> values, Double confidence) {
            this.timestamp = timestamp;
            this.values = values;
            this.confidence = confidence;
        }
        
        // Getter和Setter方法
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public Map<String, Double> getValues() { return values; }
        public void setValues(Map<String, Double> values) { this.values = values; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
    
    // 内部类：统计摘要
    @Schema(description = "统计摘要信息")
    public static class StatisticalSummary {
        @JsonProperty("parameterStats")
        private Map<String, ParameterStats> parameterStats;
        
        @JsonProperty("overallQualityTrend")
        private String overallQualityTrend;
        
        @JsonProperty("anomalyCount")
        private Integer anomalyCount;
        
        public StatisticalSummary() {}
        
        // Getter和Setter方法
        public Map<String, ParameterStats> getParameterStats() { return parameterStats; }
        public void setParameterStats(Map<String, ParameterStats> parameterStats) { this.parameterStats = parameterStats; }
        public String getOverallQualityTrend() { return overallQualityTrend; }
        public void setOverallQualityTrend(String overallQualityTrend) { this.overallQualityTrend = overallQualityTrend; }
        public Integer getAnomalyCount() { return anomalyCount; }
        public void setAnomalyCount(Integer anomalyCount) { this.anomalyCount = anomalyCount; }
    }
    
    // 内部类：参数统计
    @Schema(description = "参数统计信息")
    public static class ParameterStats {
        @JsonProperty("mean")
        private Double mean;
        
        @JsonProperty("min")
        private Double min;
        
        @JsonProperty("max")
        private Double max;
        
        @JsonProperty("standardDeviation")
        private Double standardDeviation;
        
        @JsonProperty("trend")
        private String trend; // "increasing", "decreasing", "stable"
        
        public ParameterStats() {}
        
        public ParameterStats(Double mean, Double min, Double max, Double standardDeviation, String trend) {
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.standardDeviation = standardDeviation;
            this.trend = trend;
        }
        
        // Getter和Setter方法
        public Double getMean() { return mean; }
        public void setMean(Double mean) { this.mean = mean; }
        public Double getMin() { return min; }
        public void setMin(Double min) { this.min = min; }
        public Double getMax() { return max; }
        public void setMax(Double max) { this.max = max; }
        public Double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(Double standardDeviation) { this.standardDeviation = standardDeviation; }
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
    }
    
    // 内部类：趋势指标
    @Schema(description = "趋势指标")
    public static class TrendIndicator {
        @JsonProperty("direction")
        private String direction; // "up", "down", "stable"
        
        @JsonProperty("strength")
        private Double strength; // 0-1之间
        
        @JsonProperty("significance")
        private Double significance; // p-value
        
        public TrendIndicator() {}
        
        public TrendIndicator(String direction, Double strength, Double significance) {
            this.direction = direction;
            this.strength = strength;
            this.significance = significance;
        }
        
        // Getter和Setter方法
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public Double getStrength() { return strength; }
        public void setStrength(Double strength) { this.strength = strength; }
        public Double getSignificance() { return significance; }
        public void setSignificance(Double significance) { this.significance = significance; }
    }
    
    // 默认构造函数
    public TrendAnalysisResponse() {
        this.analysisTimestamp = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    public AnalysisInfo getAnalysisInfo() {
        return analysisInfo;
    }
    
    public void setAnalysisInfo(AnalysisInfo analysisInfo) {
        this.analysisInfo = analysisInfo;
    }
    
    public List<TrendDataPoint> getHistoricalTrends() {
        return historicalTrends;
    }
    
    public void setHistoricalTrends(List<TrendDataPoint> historicalTrends) {
        this.historicalTrends = historicalTrends;
    }
    
    public List<TrendDataPoint> getForecastTrends() {
        return forecastTrends;
    }
    
    public void setForecastTrends(List<TrendDataPoint> forecastTrends) {
        this.forecastTrends = forecastTrends;
    }
    
    public StatisticalSummary getStatisticalSummary() {
        return statisticalSummary;
    }
    
    public void setStatisticalSummary(StatisticalSummary statisticalSummary) {
        this.statisticalSummary = statisticalSummary;
    }
    
    public Map<String, TrendIndicator> getTrendIndicators() {
        return trendIndicators;
    }
    
    public void setTrendIndicators(Map<String, TrendIndicator> trendIndicators) {
        this.trendIndicators = trendIndicators;
    }
    
    public LocalDateTime getAnalysisTimestamp() {
        return analysisTimestamp;
    }
    
    public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    @Override
    public String toString() {
        return "TrendAnalysisResponse{" +
                "analysisInfo=" + analysisInfo +
                ", historicalTrends=" + (historicalTrends != null ? historicalTrends.size() + " points" : "null") +
                ", forecastTrends=" + (forecastTrends != null ? forecastTrends.size() + " points" : "null") +
                ", statisticalSummary=" + statisticalSummary +
                ", trendIndicators=" + trendIndicators +
                ", analysisTimestamp=" + analysisTimestamp +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}