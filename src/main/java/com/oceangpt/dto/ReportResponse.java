package com.oceangpt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报告生成响应DTO
 */
@Schema(description = "水质报告生成响应")
public class ReportResponse {
    
    @Schema(description = "操作是否成功", example = "true")
    private boolean success;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "纬度", example = "39.0")
    private Double latitude;
    
    @Schema(description = "经度", example = "119.0")
    private Double longitude;
    
    @Schema(description = "报告生成时间")
    private LocalDateTime reportTimestamp;
    
    @Schema(description = "报告类型", example = "WATER_QUALITY_ANALYSIS")
    private String reportType;
    
    @Schema(description = "报告标题")
    private String title;
    
    @Schema(description = "执行摘要")
    private String executiveSummary;
    
    @Schema(description = "详细分析")
    private String detailedAnalysis;
    
    @Schema(description = "建议列表")
    private List<String> recommendations;
    
    @Schema(description = "风险评估")
    private String riskAssessment;
    
    @Schema(description = "预测数据")
    private Map<String, PredictionResponse> predictions;
    
    @Schema(description = "处理时间（毫秒）", example = "1500")
    private Integer processingTimeMs;
    
    @Schema(description = "报告版本", example = "1.0")
    private String reportVersion = "1.0";
    
    @Schema(description = "数据来源")
    private String dataSource = "Sentinel-2/3 + EndToEndRegressionModel";
    
    @Schema(description = "置信度评级")
    private String confidenceRating;
    
    @Schema(description = "质量控制信息")
    private QualityControl qualityControl;
    
    @Schema(description = "报告ID", example = "REPORT-20240115-123456-A1B2C3D4")
    private String reportId;
    
    @Schema(description = "报告生成时间")
    private LocalDateTime generatedAt;
    
    @Schema(description = "完整报告内容（Markdown格式）")
    private String content;
    
    // 构造函数
    public ReportResponse() {}
    
    public ReportResponse(boolean success) {
        this.success = success;
    }
    
    // 质量控制内部类
    @Schema(description = "质量控制信息")
    public static class QualityControl {
        @Schema(description = "数据完整性检查", example = "PASSED")
        private String dataIntegrityCheck;
        
        @Schema(description = "模型验证状态", example = "VALIDATED")
        private String modelValidationStatus;
        
        @Schema(description = "异常值检测", example = "NO_ANOMALIES")
        private String anomalyDetection;
        
        @Schema(description = "质量评分 (0-100)", example = "95")
        private Integer qualityScore;
        
        public QualityControl() {}
        
        public QualityControl(String dataIntegrityCheck, String modelValidationStatus, 
                            String anomalyDetection, Integer qualityScore) {
            this.dataIntegrityCheck = dataIntegrityCheck;
            this.modelValidationStatus = modelValidationStatus;
            this.anomalyDetection = anomalyDetection;
            this.qualityScore = qualityScore;
        }
        
        // Getters and Setters
        public String getDataIntegrityCheck() {
            return dataIntegrityCheck;
        }
        
        public void setDataIntegrityCheck(String dataIntegrityCheck) {
            this.dataIntegrityCheck = dataIntegrityCheck;
        }
        
        public String getModelValidationStatus() {
            return modelValidationStatus;
        }
        
        public void setModelValidationStatus(String modelValidationStatus) {
            this.modelValidationStatus = modelValidationStatus;
        }
        
        public String getAnomalyDetection() {
            return anomalyDetection;
        }
        
        public void setAnomalyDetection(String anomalyDetection) {
            this.anomalyDetection = anomalyDetection;
        }
        
        public Integer getQualityScore() {
            return qualityScore;
        }
        
        public void setQualityScore(Integer qualityScore) {
            this.qualityScore = qualityScore;
        }
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
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public LocalDateTime getReportTimestamp() {
        return reportTimestamp;
    }
    
    public void setReportTimestamp(LocalDateTime reportTimestamp) {
        this.reportTimestamp = reportTimestamp;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
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
    
    public String getDetailedAnalysis() {
        return detailedAnalysis;
    }
    
    public void setDetailedAnalysis(String detailedAnalysis) {
        this.detailedAnalysis = detailedAnalysis;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getRiskAssessment() {
        return riskAssessment;
    }
    
    public void setRiskAssessment(String riskAssessment) {
        this.riskAssessment = riskAssessment;
    }
    
    public Map<String, PredictionResponse> getPredictions() {
        return predictions;
    }
    
    public void setPredictions(Map<String, PredictionResponse> predictions) {
        this.predictions = predictions;
    }
    
    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public String getReportVersion() {
        return reportVersion;
    }
    
    public void setReportVersion(String reportVersion) {
        this.reportVersion = reportVersion;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    public String getConfidenceRating() {
        return confidenceRating;
    }
    
    public void setConfidenceRating(String confidenceRating) {
        this.confidenceRating = confidenceRating;
    }
    
    public QualityControl getQualityControl() {
        return qualityControl;
    }
    
    public void setQualityControl(QualityControl qualityControl) {
        this.qualityControl = qualityControl;
    }
    
    public String getReportId() {
        return reportId;
    }
    
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    @Override
    public String toString() {
        return "ReportResponse{" +
                "success=" + success +
                ", reportId='" + reportId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", reportType='" + reportType + '\'' +
                ", title='" + title + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}