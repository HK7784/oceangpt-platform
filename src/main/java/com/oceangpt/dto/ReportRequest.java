package com.oceangpt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 报告生成请求DTO
 */
@Schema(description = "水质报告生成请求")
public class ReportRequest {
    
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    @Schema(description = "纬度", example = "39.0", required = true)
    private Double latitude;
    
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    @Schema(description = "经度", example = "119.0", required = true)
    private Double longitude;
    
    // S2光谱数据 (8个波段)
    @Schema(description = "Sentinel-2 B2波段反射率", example = "0.05")
    private Double s2B2;
    
    @Schema(description = "Sentinel-2 B3波段反射率", example = "0.06")
    private Double s2B3;
    
    @Schema(description = "Sentinel-2 B4波段反射率", example = "0.04")
    private Double s2B4;
    
    @Schema(description = "Sentinel-2 B5波段反射率", example = "0.08")
    private Double s2B5;
    
    @Schema(description = "Sentinel-2 B6波段反射率", example = "0.09")
    private Double s2B6;
    
    @Schema(description = "Sentinel-2 B7波段反射率", example = "0.07")
    private Double s2B7;
    
    @Schema(description = "Sentinel-2 B8波段反射率", example = "0.12")
    private Double s2B8;
    
    @Schema(description = "Sentinel-2 B8A波段反射率", example = "0.10")
    private Double s2B8A;
    
    // S3光谱数据 (8个波段)
    @Schema(description = "Sentinel-3 Oa01波段反射率", example = "0.03")
    private Double s3Oa01;
    
    @Schema(description = "Sentinel-3 Oa02波段反射率", example = "0.04")
    private Double s3Oa02;
    
    @Schema(description = "Sentinel-3 Oa03波段反射率", example = "0.05")
    private Double s3Oa03;
    
    @Schema(description = "Sentinel-3 Oa04波段反射率", example = "0.06")
    private Double s3Oa04;
    
    @Schema(description = "Sentinel-3 Oa05波段反射率", example = "0.07")
    private Double s3Oa05;
    
    @Schema(description = "Sentinel-3 Oa06波段反射率", example = "0.08")
    private Double s3Oa06;
    
    @Schema(description = "Sentinel-3 Oa07波段反射率", example = "0.09")
    private Double s3Oa07;
    
    @Schema(description = "Sentinel-3 Oa08波段反射率", example = "0.10")
    private Double s3Oa08;
    
    // 物理要素
    @Schema(description = "叶绿素浓度 (mg/m³)", example = "2.5")
    private Double chlNN;
    
    @Schema(description = "总悬浮物浓度 (mg/L)", example = "15.0")
    private Double tsmNN;
    
    @Schema(description = "叶绿素浓度 (mg/m³)", example = "2.5")
    private Double chlorophyllConcentration;
    
    @Schema(description = "浊度 (NTU)", example = "15.3")
    private Double turbidity;
    
    @Schema(description = "报告类型", example = "COMPREHENSIVE", allowableValues = {"COMPREHENSIVE", "SUMMARY", "TECHNICAL"})
    private String reportType = "COMPREHENSIVE";
    
    @Schema(description = "报告语言", example = "zh-CN", allowableValues = {"zh-CN", "en-US"})
    private String language = "zh-CN";

    @Schema(description = "RAG检索文档", hidden = true)
    private java.util.List<java.util.Map<String, Object>> ragDocuments;
    
    // 构造函数
    public ReportRequest() {}
    
    public ReportRequest(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getters and Setters
    public java.util.List<java.util.Map<String, Object>> getRagDocuments() {
        return ragDocuments;
    }

    public void setRagDocuments(java.util.List<java.util.Map<String, Object>> ragDocuments) {
        this.ragDocuments = ragDocuments;
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
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    @Override
    public String toString() {
        return "ReportRequest{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", reportType='" + reportType + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
