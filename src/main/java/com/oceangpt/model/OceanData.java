package com.oceangpt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 海洋数据实体类
 */
@Entity
@Table(name = "ocean_data")
public class OceanData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @NotNull
    @Column(name = "latitude")
    private Double latitude;
    
    @NotNull
    @Column(name = "longitude")
    private Double longitude;
    
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
    
    @Column(name = "turbidity")
    private Double turbidity;
    
    @Column(name = "pollution_index")
    private Double pollutionIndex;
    
    @Column(name = "data_source")
    private String dataSource;
    
    // 构造函数
    public OceanData() {}
    
    public OceanData(LocalDateTime timestamp, Double latitude, Double longitude) {
        this.timestamp = timestamp;
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
    
    public Double getTurbidity() {
        return turbidity;
    }
    
    public void setTurbidity(Double turbidity) {
        this.turbidity = turbidity;
    }
    
    public Double getPollutionIndex() {
        return pollutionIndex;
    }
    
    public void setPollutionIndex(Double pollutionIndex) {
        this.pollutionIndex = pollutionIndex;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}