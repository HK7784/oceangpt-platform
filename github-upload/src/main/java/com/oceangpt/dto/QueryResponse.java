package com.oceangpt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 自然语言查询响应DTO
 */
@Schema(description = "自然语言查询响应")
public class QueryResponse {
    
    @Schema(description = "操作是否成功", example = "true")
    private boolean success;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "查询ID")
    private String queryId;
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "响应时间")
    private LocalDateTime responseTimestamp;
    
    @Schema(description = "主要回答")
    private String answer;
    
    @Schema(description = "置信度 (0-1)", example = "0.85")
    private Double confidence;
    
    @Schema(description = "查询类型", example = "EXPLANATION")
    private String queryType;
    
    @Schema(description = "相关数据")
    private Map<String, Object> relatedData;
    
    @Schema(description = "支持证据")
    private List<String> supportingEvidence;
    
    @Schema(description = "相关建议")
    private List<String> recommendations;
    
    @Schema(description = "数据来源")
    private List<String> dataSources;
    
    @Schema(description = "处理时间（毫秒）", example = "800")
    private Integer processingTimeMs;
    
    @Schema(description = "响应语言", example = "zh-CN")
    private String language;
    
    @Schema(description = "技术细节")
    private TechnicalDetails technicalDetails;
    
    @Schema(description = "后续建议查询")
    private List<String> suggestedFollowUpQueries;
    
    @Schema(description = "相关参数")
    private List<String> relatedParameters;
    
    @Schema(description = "地理上下文")
    private GeographicContext geographicContext;
    
    // 技术细节内部类
    @Schema(description = "技术细节信息")
    public static class TechnicalDetails {
        @Schema(description = "使用的模型")
        private String modelUsed;
        
        @Schema(description = "算法版本")
        private String algorithmVersion;
        
        @Schema(description = "数据质量评分")
        private Integer dataQualityScore;
        
        @Schema(description = "计算方法")
        private String calculationMethod;
        
        @Schema(description = "不确定性范围")
        private String uncertaintyRange;
        
        public TechnicalDetails() {}
        
        // Getters and Setters
        public String getModelUsed() {
            return modelUsed;
        }
        
        public void setModelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
        }
        
        public String getAlgorithmVersion() {
            return algorithmVersion;
        }
        
        public void setAlgorithmVersion(String algorithmVersion) {
            this.algorithmVersion = algorithmVersion;
        }
        
        public Integer getDataQualityScore() {
            return dataQualityScore;
        }
        
        public void setDataQualityScore(Integer dataQualityScore) {
            this.dataQualityScore = dataQualityScore;
        }
        
        public String getCalculationMethod() {
            return calculationMethod;
        }
        
        public void setCalculationMethod(String calculationMethod) {
            this.calculationMethod = calculationMethod;
        }
        
        public String getUncertaintyRange() {
            return uncertaintyRange;
        }
        
        public void setUncertaintyRange(String uncertaintyRange) {
            this.uncertaintyRange = uncertaintyRange;
        }
    }
    
    // 地理上下文内部类
    @Schema(description = "地理上下文信息")
    public static class GeographicContext {
        @Schema(description = "区域名称")
        private String regionName;
        
        @Schema(description = "水体类型")
        private String waterBodyType;
        
        @Schema(description = "环境特征")
        private List<String> environmentalFeatures;
        
        @Schema(description = "附近污染源")
        private List<String> nearbyPollutionSources;
        
        public GeographicContext() {}
        
        // Getters and Setters
        public String getRegionName() {
            return regionName;
        }
        
        public void setRegionName(String regionName) {
            this.regionName = regionName;
        }
        
        public String getWaterBodyType() {
            return waterBodyType;
        }
        
        public void setWaterBodyType(String waterBodyType) {
            this.waterBodyType = waterBodyType;
        }
        
        public List<String> getEnvironmentalFeatures() {
            return environmentalFeatures;
        }
        
        public void setEnvironmentalFeatures(List<String> environmentalFeatures) {
            this.environmentalFeatures = environmentalFeatures;
        }
        
        public List<String> getNearbyPollutionSources() {
            return nearbyPollutionSources;
        }
        
        public void setNearbyPollutionSources(List<String> nearbyPollutionSources) {
            this.nearbyPollutionSources = nearbyPollutionSources;
        }
    }
    
    // 构造函数
    public QueryResponse() {}
    
    public QueryResponse(boolean success) {
        this.success = success;
    }
    
    public QueryResponse(boolean success, String answer) {
        this.success = success;
        this.answer = answer;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getQueryId() {
        return queryId;
    }
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public LocalDateTime getResponseTimestamp() {
        return responseTimestamp;
    }
    
    public void setResponseTimestamp(LocalDateTime responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getQueryType() {
        return queryType;
    }
    
    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
    
    public Map<String, Object> getRelatedData() {
        return relatedData;
    }
    
    public void setRelatedData(Map<String, Object> relatedData) {
        this.relatedData = relatedData;
    }
    
    public List<String> getSupportingEvidence() {
        return supportingEvidence;
    }
    
    public void setSupportingEvidence(List<String> supportingEvidence) {
        this.supportingEvidence = supportingEvidence;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public List<String> getDataSources() {
        return dataSources;
    }
    
    public void setDataSources(List<String> dataSources) {
        this.dataSources = dataSources;
    }
    
    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public TechnicalDetails getTechnicalDetails() {
        return technicalDetails;
    }
    
    public void setTechnicalDetails(TechnicalDetails technicalDetails) {
        this.technicalDetails = technicalDetails;
    }
    
    public List<String> getSuggestedFollowUpQueries() {
        return suggestedFollowUpQueries;
    }
    
    public void setSuggestedFollowUpQueries(List<String> suggestedFollowUpQueries) {
        this.suggestedFollowUpQueries = suggestedFollowUpQueries;
    }
    
    public List<String> getRelatedParameters() {
        return relatedParameters;
    }
    
    public void setRelatedParameters(List<String> relatedParameters) {
        this.relatedParameters = relatedParameters;
    }
    
    public GeographicContext getGeographicContext() {
        return geographicContext;
    }
    
    public void setGeographicContext(GeographicContext geographicContext) {
        this.geographicContext = geographicContext;
    }
    
    @Override
    public String toString() {
        return "QueryResponse{" +
                "success=" + success +
                ", queryId='" + queryId + '\'' +
                ", answer='" + answer + '\'' +
                ", confidence=" + confidence +
                ", queryType='" + queryType + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}