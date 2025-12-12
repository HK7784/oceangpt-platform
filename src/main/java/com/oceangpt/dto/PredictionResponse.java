package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 预测响应DTO
 */
@Schema(description = "水质预测响应")
public class PredictionResponse {
    
    @Schema(description = "营养盐浓度 (DIN)", example = "0.45")
    @JsonProperty("nutrient")
    private Double nutrient;
    
    @Schema(description = "pH值", example = "8.1")
    @JsonProperty("ph")
    private Double ph;
    
    @Schema(description = "置信度", example = "0.85")
    @JsonProperty("confidence")
    private Double confidence;
    
    @Schema(description = "错误信息")
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @Schema(description = "预测结果是否成功")
    @JsonProperty("success")
    private boolean success = true;

    // 新增字段
    @Schema(description = "海表温度", example = "25.4")
    @JsonProperty("seaSurfaceTemperature")
    private Double seaSurfaceTemperature;

    @Schema(description = "盐度", example = "34.5")
    @JsonProperty("salinity")
    private Double salinity;

    @Schema(description = "溶解氧", example = "6.5")
    @JsonProperty("dissolvedOxygen")
    private Double dissolvedOxygen;

    @Schema(description = "叶绿素浓度", example = "1.2")
    @JsonProperty("chlorophyllConcentration")
    private Double chlorophyllConcentration;
    
    @Schema(description = "DIN浓度", example = "0.15")
    @JsonProperty("dinLevel")
    private Double dinLevel;
    
    @Schema(description = "SRP浓度", example = "0.02")
    @JsonProperty("srpLevel")
    private Double srpLevel;
    
    @Schema(description = "pH预测值", example = "8.15")
    @JsonProperty("phLevel")
    private Double phLevel;
    
    @Schema(description = "经度", example = "120.5")
    @JsonProperty("longitude")
    private Double longitude;
    
    @Schema(description = "纬度", example = "30.5")
    @JsonProperty("latitude")
    private Double latitude;
    
    @Schema(description = "水质类别", example = "I类")
    @JsonProperty("waterQualityLevel")
    private String waterQualityLevel;
    
    @Schema(description = "污染指数", example = "0.35")
    @JsonProperty("pollutionIndex")
    private Double pollutionIndex;
    
    @Schema(description = "水质等级", example = "良好")
    @JsonProperty("qualityLevel")
    private String qualityLevel;
    
    @Schema(description = "预测时间戳", example = "2024-01-15T10:30:00")
    @JsonProperty("predictionTimestamp")
    private LocalDateTime predictionTimestamp;
    
    @Schema(description = "模型版本", example = "v1.0")
    @JsonProperty("modelVersion")
    private String modelVersion;

    // 新增方法：为了解决 "cannot find symbol: method getChlLevel()"
    public Double getChlLevel() {
        return chlorophyllConcentration;
    }
    
    @Schema(description = "处理时间（毫秒）", example = "150")
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;
    
    @Schema(description = "额外信息")
    @JsonProperty("additionalInfo")
    private Map<String, Object> additionalInfo;
    
    @Schema(description = "警告信息")
    @JsonProperty("warnings")
    private String[] warnings;
    
    // 默认构造函数
    public PredictionResponse() {
        this.predictionTimestamp = LocalDateTime.now();
    }
    
    // 构造函数
    public PredictionResponse(Double nutrient, Double ph, Double confidence) {
        this();
        this.nutrient = nutrient;
        this.ph = ph;
        this.confidence = confidence;
    }
    
    // 全参构造函数
    public PredictionResponse(Double nutrient, Double ph, Double confidence, 
                            Double seaSurfaceTemperature, Double salinity, 
                            Double dissolvedOxygen, Double chlorophyllConcentration,
                            Double pollutionIndex, String qualityLevel,
                            String modelVersion, Long processingTimeMs,
                            Map<String, Object> additionalInfo, String[] warnings) {
        this();
        this.nutrient = nutrient;
        this.ph = ph;
        this.confidence = confidence;
        this.seaSurfaceTemperature = seaSurfaceTemperature;
        this.salinity = salinity;
        this.dissolvedOxygen = dissolvedOxygen;
        this.chlorophyllConcentration = chlorophyllConcentration;
        this.pollutionIndex = pollutionIndex;
        this.qualityLevel = qualityLevel;
        this.modelVersion = modelVersion;
        this.processingTimeMs = processingTimeMs;
        this.additionalInfo = additionalInfo;
        this.warnings = warnings;
    }
    
    // Getter和Setter方法
    public Double getNutrient() {
        return nutrient;
    }
    
    public void setNutrient(Double nutrient) {
        this.nutrient = nutrient;
    }
    
    public Double getPh() {
        return ph;
    }
    
    public void setPh(Double ph) {
        this.ph = ph;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public Double getSeaSurfaceTemperature() {
        return seaSurfaceTemperature;
    }
    
    public void setSeaSurfaceTemperature(Double seaSurfaceTemperature) {
        this.seaSurfaceTemperature = seaSurfaceTemperature;
    }
    
    public Double getSalinity() {
        return salinity;
    }
    
    public void setSalinity(Double salinity) {
        this.salinity = salinity;
    }
    
    public Double getDissolvedOxygen() {
        return dissolvedOxygen;
    }
    
    public void setDissolvedOxygen(Double dissolvedOxygen) {
        this.dissolvedOxygen = dissolvedOxygen;
    }
    
    public Double getChlorophyllConcentration() {
        return chlorophyllConcentration;
    }
    
    public void setChlorophyllConcentration(Double chlorophyllConcentration) {
        this.chlorophyllConcentration = chlorophyllConcentration;
    }
    
    public Double getPollutionIndex() {
        return pollutionIndex;
    }
    
    public void setPollutionIndex(Double pollutionIndex) {
        this.pollutionIndex = pollutionIndex;
    }
    
    public String getQualityLevel() {
        return qualityLevel;
    }
    
    public void setQualityLevel(String qualityLevel) {
        this.qualityLevel = qualityLevel;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Double getDinLevel() {
        return dinLevel;
    }

    public void setDinLevel(Double dinLevel) {
        this.dinLevel = dinLevel;
    }

    public Double getSrpLevel() {
        return srpLevel;
    }

    public void setSrpLevel(Double srpLevel) {
        this.srpLevel = srpLevel;
    }

    public Double getPhLevel() {
        return phLevel;
    }

    public void setPhLevel(Double phLevel) {
        this.phLevel = phLevel;
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

    public String getWaterQualityLevel() {
        return waterQualityLevel;
    }

    public void setWaterQualityLevel(String waterQualityLevel) {
        this.waterQualityLevel = waterQualityLevel;
    }

    public LocalDateTime getPredictionTimestamp() {
        return predictionTimestamp;
    }

    public void setPredictionTimestamp(LocalDateTime predictionTimestamp) {
        this.predictionTimestamp = predictionTimestamp;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String[] getWarnings() {
        return warnings;
    }

    public void setWarnings(String[] warnings) {
        this.warnings = warnings;
    }
}
