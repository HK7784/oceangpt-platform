package com.oceangpt.dto;

import java.time.LocalDateTime;

/**
 * 卫星数据请求DTO
 */
public class SatelliteDataRequest {
    
    private Double latitude;
    private Double longitude;
    private LocalDateTime dateTime;
    private String apiKey;
    private String dataType; // "sentinel2" or "sentinel3" or "both"
    private Integer cloudCoverThreshold; // 云覆盖阈值
    private Integer timeRangeDays; // 时间范围（天）
    
    public SatelliteDataRequest() {
        this.dataType = "both";
        this.cloudCoverThreshold = 20; // 默认20%云覆盖阈值
        this.timeRangeDays = 7; // 默认7天时间范围
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
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public Integer getCloudCoverThreshold() {
        return cloudCoverThreshold;
    }
    
    public void setCloudCoverThreshold(Integer cloudCoverThreshold) {
        this.cloudCoverThreshold = cloudCoverThreshold;
    }
    
    public Integer getTimeRangeDays() {
        return timeRangeDays;
    }
    
    public void setTimeRangeDays(Integer timeRangeDays) {
        this.timeRangeDays = timeRangeDays;
    }
    
    @Override
    public String toString() {
        return "SatelliteDataRequest{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", dateTime=" + dateTime +
                ", dataType='" + dataType + '\'' +
                ", cloudCoverThreshold=" + cloudCoverThreshold +
                ", timeRangeDays=" + timeRangeDays +
                '}';
    }
}