package com.oceangpt.dto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 卫星数据响应DTO
 */
public class SatelliteDataResponse {
    
    private boolean success;
    private String message;
    private Double latitude;
    private Double longitude;
    private LocalDateTime dateTime;
    private Map<String, Double> s2Data; // Sentinel-2 光谱数据
    private Map<String, Double> s3Data; // Sentinel-3 光谱数据
    private Double chlNN; // 叶绿素浓度神经网络预测值
    private Double tsmNN; // 总悬浮物浓度神经网络预测值
    private String dataSource; // 数据源
    private Double qualityScore; // 数据质量评分 (0-1)
    private Integer cloudCover; // 云覆盖百分比
    private Timestamp timestamp; // 数据获取时间戳
    private String errorCode; // 错误代码
    
    public SatelliteDataResponse() {
        this.success = false;
        this.qualityScore = 0.0;
        this.cloudCover = 0;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
    
    public Map<String, Double> getS2Data() {
        return s2Data;
    }
    
    public void setS2Data(Map<String, Double> s2Data) {
        this.s2Data = s2Data;
    }
    
    public Map<String, Double> getS3Data() {
        return s3Data;
    }
    
    public void setS3Data(Map<String, Double> s3Data) {
        this.s3Data = s3Data;
    }
    
    public Double getChlNN() {
        return chlNN;
    }
    
    public void setChlNN(Double chlNN) {
        this.chlNN = chlNN;
    }
    
    public Double getTsmNN() {
        return tsmNN;
    }
    
    public void setTsmNN(Double tsmNN) {
        this.tsmNN = tsmNN;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    public Double getQualityScore() {
        return qualityScore;
    }
    
    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }
    
    public Integer getCloudCover() {
        return cloudCover;
    }
    
    public void setCloudCover(Integer cloudCover) {
        this.cloudCover = cloudCover;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * 检查是否有有效的Sentinel-2数据
     */
    public boolean hasValidS2Data() {
        return s2Data != null && !s2Data.isEmpty() && 
               s2Data.values().stream().anyMatch(v -> v != null && v > 0);
    }
    
    /**
     * 检查是否有有效的Sentinel-3数据
     */
    public boolean hasValidS3Data() {
        return s3Data != null && !s3Data.isEmpty() && 
               s3Data.values().stream().anyMatch(v -> v != null && v > 0);
    }
    
    /**
     * 获取数据完整性评分
     */
    public double getDataCompleteness() {
        double s2Completeness = hasValidS2Data() ? 0.5 : 0.0;
        double s3Completeness = hasValidS3Data() ? 0.5 : 0.0;
        return s2Completeness + s3Completeness;
    }
    
    /**
     * 创建错误响应
     */
    public static SatelliteDataResponse createErrorResponse(String message, String errorCode) {
        SatelliteDataResponse response = new SatelliteDataResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return response;
    }
    
    /**
     * 创建成功响应
     */
    public static SatelliteDataResponse createSuccessResponse(Double latitude, Double longitude, 
                                                             LocalDateTime dateTime) {
        SatelliteDataResponse response = new SatelliteDataResponse();
        response.setSuccess(true);
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setDateTime(dateTime);
        response.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return response;
    }
    
    @Override
    public String toString() {
        return "SatelliteDataResponse{" +
                "success=" + success +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", dateTime=" + dateTime +
                ", dataSource='" + dataSource + '\'' +
                ", qualityScore=" + qualityScore +
                ", cloudCover=" + cloudCover +
                ", chlNN=" + chlNN +
                ", tsmNN=" + tsmNN +
                ", hasS2Data=" + hasValidS2Data() +
                ", hasS3Data=" + hasValidS3Data() +
                '}';
    }
}