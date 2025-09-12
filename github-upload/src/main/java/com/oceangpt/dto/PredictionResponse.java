package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 水质预测响应DTO
 * 用于返回预测结果给客户端
 */
@Schema(description = "水质预测响应结果")
public class PredictionResponse {
    
    @Schema(description = "请求是否成功", example = "true")
    @JsonProperty("success")
    private boolean success;
    
    @Schema(description = "错误信息", example = "")
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @Schema(description = "纬度", example = "39.9042")
    @JsonProperty("latitude")
    private Double latitude;
    
    @Schema(description = "经度", example = "116.4074")
    @JsonProperty("longitude")
    private Double longitude;
    
    @Schema(description = "DIN浓度预测值 (mg/L)", example = "0.05")
    @JsonProperty("dinLevel")
    private Double dinLevel;
    
    @Schema(description = "DIN单位", example = "mg/L")
    @JsonProperty("dinUnit")
    private String dinUnit;
    
    @Schema(description = "SRP浓度预测值 (mg/L)", example = "0.02")
    @JsonProperty("srpLevel")
    private Double srpLevel;
    
    @Schema(description = "SRP单位", example = "mg/L")
    @JsonProperty("srpUnit")
    private String srpUnit;
    
    @Schema(description = "pH值预测", example = "8.1")
    @JsonProperty("phLevel")
    private Double phLevel;
    
    @Schema(description = "pH单位", example = "")
    @JsonProperty("phUnit")
    private String phUnit;
    
    @Schema(description = "水质等级", example = "良好")
    @JsonProperty("waterQualityLevel")
    private String waterQualityLevel;
    
    @Schema(description = "营养盐浓度预测值", example = "0.5")
    @JsonProperty("nutrient")
    private Double nutrient;
    
    @Schema(description = "pH值预测", example = "7.8")
    @JsonProperty("ph")
    private Double ph;
    
    @Schema(description = "预测置信度", example = "0.95")
    @JsonProperty("confidence")
    private Double confidence;
    
    @Schema(description = "海表温度预测", example = "15.2")
    @JsonProperty("seaSurfaceTemperature")
    private Double seaSurfaceTemperature;
    
    @Schema(description = "盐度预测", example = "35.1")
    @JsonProperty("salinity")
    private Double salinity;
    
    @Schema(description = "溶解氧预测", example = "8.2")
    @JsonProperty("dissolvedOxygen")
    private Double dissolvedOxygen;
    
    @Schema(description = "叶绿素浓度预测", example = "0.6")
    @JsonProperty("chlorophyllConcentration")
    private Double chlorophyllConcentration;
    
    @Schema(description = "污染指数", example = "0.3")
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
    
    // 新增的getter和setter方法
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
    
    public Double getDinLevel() {
        return dinLevel;
    }
    
    public void setDinLevel(Double dinLevel) {
        this.dinLevel = dinLevel;
    }
    
    public String getDinUnit() {
        return dinUnit;
    }
    
    public void setDinUnit(String dinUnit) {
        this.dinUnit = dinUnit;
    }
    
    public Double getSrpLevel() {
        return srpLevel;
    }
    
    public void setSrpLevel(Double srpLevel) {
        this.srpLevel = srpLevel;
    }
    
    public String getSrpUnit() {
        return srpUnit;
    }
    
    public void setSrpUnit(String srpUnit) {
        this.srpUnit = srpUnit;
    }
    
    public Double getPhLevel() {
        return phLevel;
    }
    
    public void setPhLevel(Double phLevel) {
        this.phLevel = phLevel;
    }
    
    public String getPhUnit() {
        return phUnit;
    }
    
    public void setPhUnit(String phUnit) {
        this.phUnit = phUnit;
    }
    
    public String getWaterQualityLevel() {
        return waterQualityLevel;
    }
    
    public void setWaterQualityLevel(String waterQualityLevel) {
        this.waterQualityLevel = waterQualityLevel;
    }
    
    public void setProcessingTimeMs(int processingTimeMs) {
        this.processingTimeMs = (long) processingTimeMs;
    }
    
    @Override
    public String toString() {
        return "PredictionResponse{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", dinLevel=" + dinLevel +
                ", dinUnit='" + dinUnit + '\'' +
                ", srpLevel=" + srpLevel +
                ", srpUnit='" + srpUnit + '\'' +
                ", phLevel=" + phLevel +
                ", phUnit='" + phUnit + '\'' +
                ", waterQualityLevel='" + waterQualityLevel + '\'' +
                ", nutrient=" + nutrient +
                ", ph=" + ph +
                ", confidence=" + confidence +
                ", seaSurfaceTemperature=" + seaSurfaceTemperature +
                ", salinity=" + salinity +
                ", dissolvedOxygen=" + dissolvedOxygen +
                ", chlorophyllConcentration=" + chlorophyllConcentration +
                ", pollutionIndex=" + pollutionIndex +
                ", qualityLevel='" + qualityLevel + '\'' +
                ", predictionTimestamp=" + predictionTimestamp +
                ", modelVersion='" + modelVersion + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                ", additionalInfo=" + additionalInfo +
                ", warnings=" + java.util.Arrays.toString(warnings) +
                '}';
    }
}