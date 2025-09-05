package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 水质预测请求DTO
 * 用于接收客户端的预测请求参数
 */
@Schema(description = "水质预测请求参数")
public class PredictionRequest {
    
    @Schema(description = "数据时间戳（Unix时间戳）", example = "1693478400", required = true)
    @NotNull(message = "时间戳不能为空")
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @Schema(description = "纬度", example = "30.5", minimum = "-90", maximum = "90", required = true)
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    @Digits(integer = 2, fraction = 6, message = "纬度格式不正确，最多2位整数6位小数")
    @JsonProperty("latitude")
    private Double latitude;
    
    @Schema(description = "经度", example = "-114.2", minimum = "-180", maximum = "180", required = true)
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    @Digits(integer = 3, fraction = 6, message = "经度格式不正确，最多3位整数6位小数")
    @JsonProperty("longitude")
    private Double longitude;
    
    @Schema(description = "预测时间", example = "2024-03-15T10:30:00")
    @JsonProperty("dateTime")
    private LocalDateTime dateTime;
    
    // 物理要素数据（端对端模型需要的核心参数）
    @DecimalMin(value = "0.0", message = "叶绿素浓度不能为负数")
    @DecimalMax(value = "100.0", message = "叶绿素浓度不能超过100 mg/m³")
    @Digits(integer = 3, fraction = 3, message = "叶绿素浓度格式不正确")
    @Schema(description = "叶绿素浓度（mg/m³）", example = "0.8", minimum = "0", maximum = "100")
    @JsonProperty("chlorophyllConcentration")
    private Double chlorophyllConcentration;
    
    @DecimalMin(value = "0.0", message = "浊度不能为负数")
    @DecimalMax(value = "1000.0", message = "浊度不能超过1000 NTU")
    @Digits(integer = 4, fraction = 3, message = "浊度格式不正确")
    @Schema(description = "浊度（NTU）", example = "25.5", minimum = "0", maximum = "1000")
    @JsonProperty("turbidity")
    private Double turbidity;
    
    // S2 MSI光谱数据字段
    @Schema(description = "S2 MSI B1波段", example = "0.04")
    @JsonProperty("s2B1")
    private Double s2B1;
    
    @Schema(description = "S2 MSI B2波段", example = "0.05")
    @JsonProperty("s2B2")
    private Double s2B2;
    
    @Schema(description = "S2 MSI B3波段", example = "0.06")
    @JsonProperty("s2B3")
    private Double s2B3;
    
    @Schema(description = "S2 MSI B4波段", example = "0.07")
    @JsonProperty("s2B4")
    private Double s2B4;
    
    @Schema(description = "S2 MSI B5波段", example = "0.08")
    @JsonProperty("s2B5")
    private Double s2B5;
    
    @Schema(description = "S2 MSI B6波段", example = "0.09")
    @JsonProperty("s2B6")
    private Double s2B6;
    
    @Schema(description = "S2 MSI B7波段", example = "0.10")
    @JsonProperty("s2B7")
    private Double s2B7;
    
    @Schema(description = "S2 MSI B8波段", example = "0.11")
    @JsonProperty("s2B8")
    private Double s2B8;
    
    @Schema(description = "S2 MSI B8A波段", example = "0.12")
    @JsonProperty("s2B8A")
    private Double s2B8A;
    
    // S3 OLCI光谱数据字段
    @Schema(description = "S3 OLCI Oa01波段", example = "0.01")
    @JsonProperty("s3Oa01")
    private Double s3Oa01;
    
    @Schema(description = "S3 OLCI Oa02波段", example = "0.02")
    @JsonProperty("s3Oa02")
    private Double s3Oa02;
    
    @Schema(description = "S3 OLCI Oa03波段", example = "0.03")
    @JsonProperty("s3Oa03")
    private Double s3Oa03;
    
    @Schema(description = "S3 OLCI Oa04波段", example = "0.04")
    @JsonProperty("s3Oa04")
    private Double s3Oa04;
    
    @Schema(description = "S3 OLCI Oa05波段", example = "0.05")
    @JsonProperty("s3Oa05")
    private Double s3Oa05;
    
    @Schema(description = "S3 OLCI Oa06波段", example = "0.06")
    @JsonProperty("s3Oa06")
    private Double s3Oa06;
    
    @Schema(description = "S3 OLCI Oa07波段", example = "0.07")
    @JsonProperty("s3Oa07")
    private Double s3Oa07;
    
    @Schema(description = "S3 OLCI Oa08波段", example = "0.08")
    @JsonProperty("s3Oa08")
    private Double s3Oa08;
    
    // 神经网络预测字段
    @Schema(description = "叶绿素神经网络预测值", example = "0.8")
    @JsonProperty("chlNN")
    private Double chlNN;
    
    @Schema(description = "总悬浮物神经网络预测值", example = "5.2")
    @JsonProperty("tsmNN")
    private Double tsmNN;
    
    @Schema(description = "额外参数")
    @JsonProperty("additionalParams")
    private Map<String, Object> additionalParams;
    
    // 默认构造函数
    public PredictionRequest() {}
    
    // 全参构造函数
    public PredictionRequest(Long timestamp, Double latitude, Double longitude, 
                           Double chlorophyllConcentration, Double turbidity,
                           Map<String, Object> additionalParams) {
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.chlorophyllConcentration = chlorophyllConcentration;
        this.turbidity = turbidity;
        this.additionalParams = additionalParams;
    }
    
    // Getter和Setter方法
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
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
    
    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }
    
    public void setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
    }
    
    // S2光谱数据的getter和setter方法
    public Double getS2B1() {
        return s2B1;
    }
    
    public void setS2B1(Double s2B1) {
        this.s2B1 = s2B1;
    }
    
    public Double getS2B2() {
        return s2B2;
    }
    
    public void setS2B2(Double s2B2) {
        this.s2B2 = s2B2;
    }
    
    public Double getS2B3() {
        return s2B3;
    }
    
    public void setS2B3(Double s2B3) {
        this.s2B3 = s2B3;
    }
    
    public Double getS2B4() {
        return s2B4;
    }
    
    public void setS2B4(Double s2B4) {
        this.s2B4 = s2B4;
    }
    
    public Double getS2B5() {
        return s2B5;
    }
    
    public void setS2B5(Double s2B5) {
        this.s2B5 = s2B5;
    }
    
    public Double getS2B6() {
        return s2B6;
    }
    
    public void setS2B6(Double s2B6) {
        this.s2B6 = s2B6;
    }
    
    public Double getS2B7() {
        return s2B7;
    }
    
    public void setS2B7(Double s2B7) {
        this.s2B7 = s2B7;
    }
    
    public Double getS2B8() {
        return s2B8;
    }
    
    public void setS2B8(Double s2B8) {
        this.s2B8 = s2B8;
    }
    
    public Double getS2B8A() {
        return s2B8A;
    }
    
    public void setS2B8A(Double s2B8A) {
        this.s2B8A = s2B8A;
    }
    
    // S3光谱数据的getter和setter方法
    public Double getS3Oa01() {
        return s3Oa01;
    }
    
    public void setS3Oa01(Double s3Oa01) {
        this.s3Oa01 = s3Oa01;
    }
    
    public Double getS3Oa02() {
        return s3Oa02;
    }
    
    public void setS3Oa02(Double s3Oa02) {
        this.s3Oa02 = s3Oa02;
    }
    
    public Double getS3Oa03() {
        return s3Oa03;
    }
    
    public void setS3Oa03(Double s3Oa03) {
        this.s3Oa03 = s3Oa03;
    }
    
    public Double getS3Oa04() {
        return s3Oa04;
    }
    
    public void setS3Oa04(Double s3Oa04) {
        this.s3Oa04 = s3Oa04;
    }
    
    public Double getS3Oa05() {
        return s3Oa05;
    }
    
    public void setS3Oa05(Double s3Oa05) {
        this.s3Oa05 = s3Oa05;
    }
    
    public Double getS3Oa06() {
        return s3Oa06;
    }
    
    public void setS3Oa06(Double s3Oa06) {
        this.s3Oa06 = s3Oa06;
    }
    
    public Double getS3Oa07() {
        return s3Oa07;
    }
    
    public void setS3Oa07(Double s3Oa07) {
        this.s3Oa07 = s3Oa07;
    }
    
    public Double getS3Oa08() {
        return s3Oa08;
    }
    
    public void setS3Oa08(Double s3Oa08) {
        this.s3Oa08 = s3Oa08;
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
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
    
    public LocalDateTime getPredictionTime() {
        return dateTime;
    }
    
    @Override
    public String toString() {
        return "PredictionRequest{" +
                "timestamp=" + timestamp +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", turbidity=" + turbidity +
                ", chlorophyllConcentration=" + chlorophyllConcentration +
                ", s2B1=" + s2B1 +
                ", s2B2=" + s2B2 +
                ", s2B3=" + s2B3 +
                ", s2B4=" + s2B4 +
                ", s2B5=" + s2B5 +
                ", s2B6=" + s2B6 +
                ", s2B7=" + s2B7 +
                ", s2B8=" + s2B8 +
                ", s2B8A=" + s2B8A +
                ", s3Oa01=" + s3Oa01 +
                ", s3Oa02=" + s3Oa02 +
                ", s3Oa03=" + s3Oa03 +
                ", s3Oa04=" + s3Oa04 +
                ", s3Oa05=" + s3Oa05 +
                ", s3Oa06=" + s3Oa06 +
                ", s3Oa07=" + s3Oa07 +
                ", s3Oa08=" + s3Oa08 +
                ", chlNN=" + chlNN +
                ", tsmNN=" + tsmNN +
                ", additionalParams=" + additionalParams +
                '}';
    }
}