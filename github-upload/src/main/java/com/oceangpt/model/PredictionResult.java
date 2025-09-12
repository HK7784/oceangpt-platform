package com.oceangpt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 预测结果实体类
 */
@Entity
@Table(name = "prediction_results")
public class PredictionResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "prediction_timestamp")
    private LocalDateTime predictionTimestamp;
    
    @NotNull
    @Column(name = "target_timestamp")
    private LocalDateTime targetTimestamp;
    
    @NotNull
    @Column(name = "latitude")
    private Double latitude;
    
    @NotNull
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "predicted_temperature")
    private Double predictedTemperature;
    
    @Column(name = "predicted_salinity")
    private Double predictedSalinity;
    
    @Column(name = "predicted_ph")
    private Double predictedPh;
    
    @Column(name = "nutrient_level")
    private Double nutrientLevel;
    
    @Column(name = "sea_surface_temperature")
    private Double seaSurfaceTemperature;
    
    @Column(name = "salinity")
    private Double salinity;
    
    @Column(name = "ph_level")
    private Double phLevel;
    
    @Column(name = "dissolved_oxygen")
    private Double dissolvedOxygen;
    
    @Column(name = "chlorophyll_concentration")
    private Double chlorophyllConcentration;
    
    @Column(name = "pollution_index")
    private Double pollutionIndex;
    
    @Column(name = "predicted_pollution_level")
    private String predictedPollutionLevel;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "model_version")
    private String modelVersion;
    
    @ElementCollection
    @CollectionTable(name = "prediction_metadata", 
                    joinColumns = @JoinColumn(name = "prediction_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    // 构造函数
    public PredictionResult() {
        this.predictionTimestamp = LocalDateTime.now();
    }
    
    public PredictionResult(LocalDateTime targetTimestamp, Double latitude, Double longitude) {
        this();
        this.targetTimestamp = targetTimestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getPredictionTimestamp() {
        return predictionTimestamp;
    }
    
    public void setPredictionTimestamp(LocalDateTime predictionTimestamp) {
        this.predictionTimestamp = predictionTimestamp;
    }
    
    public LocalDateTime getTargetTimestamp() {
        return targetTimestamp;
    }
    
    public void setTargetTimestamp(LocalDateTime targetTimestamp) {
        this.targetTimestamp = targetTimestamp;
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
    
    public Double getPredictedTemperature() {
        return predictedTemperature;
    }
    
    public void setPredictedTemperature(Double predictedTemperature) {
        this.predictedTemperature = predictedTemperature;
    }
    
    public Double getPredictedSalinity() {
        return predictedSalinity;
    }
    
    public void setPredictedSalinity(Double predictedSalinity) {
        this.predictedSalinity = predictedSalinity;
    }
    
    public Double getPredictedPh() {
        return predictedPh;
    }
    
    public void setPredictedPh(Double predictedPh) {
        this.predictedPh = predictedPh;
    }
    
    public String getPredictedPollutionLevel() {
        return predictedPollutionLevel;
    }
    
    public void setPredictedPollutionLevel(String predictedPollutionLevel) {
        this.predictedPollutionLevel = predictedPollutionLevel;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public String getModelVersion() {
        return modelVersion;
    }
    
    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    // 新增字段的getter和setter方法
    public Double getNutrientLevel() {
        return nutrientLevel;
    }
    
    public void setNutrientLevel(Double nutrientLevel) {
        this.nutrientLevel = nutrientLevel;
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
    
    public Double getPhLevel() {
        return phLevel;
    }
    
    public void setPhLevel(Double phLevel) {
        this.phLevel = phLevel;
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
    
    // 为了兼容性，添加别名方法
    public Double getConfidence() {
        return confidenceScore;
    }
    
    public void setConfidence(Double confidence) {
        this.confidenceScore = confidence;
    }
}