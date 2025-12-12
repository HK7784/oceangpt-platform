package com.oceangpt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 报告生成响应DTO
 */
@Schema(description = "水质分析报告响应")
public class ReportResponse {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "错误消息")
    private String errorMessage;
    
    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "报告ID")
    private String reportId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "执行摘要")
    private String executiveSummary;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "报告生成时间")
    private LocalDateTime reportTimestamp;

    @Schema(description = "水质等级")
    private String waterQualityGrade;

    @Schema(description = "水质状况描述")
    private String waterQualityCondition;

    @Schema(description = "主要污染物")
    private String primaryPollutants;

    @Schema(description = "详细分析")
    private String detailedAnalysis;

    @Schema(description = "建议措施")
    private String recommendations;

    @Schema(description = "环境影响评估")
    private String environmentalImpact;
    
    @Schema(description = "时空分析")
    private String spatialTemporalAnalysis;
    
    @Schema(description = "置信度")
    private Double confidence;
    
    @Schema(description = "使用的模型版本")
    private String modelVersion;
    
    @Schema(description = "额外信息")
    private Map<String, Object> additionalInfo;

    @Schema(description = "处理时间(ms)")
    private Integer processingTimeMs;

    public ReportResponse() {
    }

    public ReportResponse(boolean success) {
        this.success = success;
    }

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
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public LocalDateTime getReportTimestamp() {
        return reportTimestamp;
    }

    public void setReportTimestamp(LocalDateTime reportTimestamp) {
        this.reportTimestamp = reportTimestamp;
    }

    public String getWaterQualityGrade() {
        return waterQualityGrade;
    }

    public void setWaterQualityGrade(String waterQualityGrade) {
        this.waterQualityGrade = waterQualityGrade;
    }

    public String getWaterQualityCondition() {
        return waterQualityCondition;
    }

    public void setWaterQualityCondition(String waterQualityCondition) {
        this.waterQualityCondition = waterQualityCondition;
    }

    public String getPrimaryPollutants() {
        return primaryPollutants;
    }

    public void setPrimaryPollutants(String primaryPollutants) {
        this.primaryPollutants = primaryPollutants;
    }

    public String getDetailedAnalysis() {
        return detailedAnalysis;
    }

    public void setDetailedAnalysis(String detailedAnalysis) {
        this.detailedAnalysis = detailedAnalysis;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getEnvironmentalImpact() {
        return environmentalImpact;
    }

    public void setEnvironmentalImpact(String environmentalImpact) {
        this.environmentalImpact = environmentalImpact;
    }

    public String getSpatialTemporalAnalysis() {
        return spatialTemporalAnalysis;
    }

    public void setSpatialTemporalAnalysis(String spatialTemporalAnalysis) {
        this.spatialTemporalAnalysis = spatialTemporalAnalysis;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    @Override
    public String toString() {
        return "ReportResponse{" +
                "success=" + success +
                ", reportId='" + reportId + '\'' +
                ", title='" + title + '\'' +
                ", waterQualityGrade='" + waterQualityGrade + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}
