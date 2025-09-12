package com.oceangpt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 自然语言查询请求DTO
 */
@Schema(description = "自然语言查询请求")
public class QueryRequest {
    
    @NotBlank(message = "查询内容不能为空")
    @Size(max = 1000, message = "查询内容不能超过1000个字符")
    @Schema(description = "用户查询内容", example = "为什么渤海DIN浓度升高？", required = true)
    private String query;
    
    @Schema(description = "查询类型", example = "EXPLANATION", allowableValues = {"EXPLANATION", "PREDICTION", "TREND", "RECOMMENDATION"})
    private String queryType = "EXPLANATION";
    
    @Schema(description = "上下文信息 - 纬度", example = "39.0")
    private Double latitude;
    
    @Schema(description = "上下文信息 - 经度", example = "119.0")
    private Double longitude;
    
    @Schema(description = "时间范围 - 开始时间", example = "2024-01-01")
    private String startDate;
    
    @Schema(description = "时间范围 - 结束时间", example = "2024-12-31")
    private String endDate;
    
    @Schema(description = "相关参数", example = "DIN", allowableValues = {"DIN", "SRP", "pH", "ALL"})
    private String parameter;
    
    @Schema(description = "响应语言", example = "zh-CN", allowableValues = {"zh-CN", "en-US"})
    private String language = "zh-CN";
    
    @Schema(description = "详细程度", example = "DETAILED", allowableValues = {"BRIEF", "DETAILED", "COMPREHENSIVE"})
    private String detailLevel = "DETAILED";
    
    @Schema(description = "是否包含技术细节", example = "false")
    private Boolean includeTechnicalDetails = false;
    
    @Schema(description = "会话ID（用于上下文连续性）")
    private String sessionId;
    
    // 构造函数
    public QueryRequest() {}
    
    public QueryRequest(String query) {
        this.query = query;
    }
    
    public QueryRequest(String query, String queryType, Double latitude, Double longitude) {
        this.query = query;
        this.queryType = queryType;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getQueryType() {
        return queryType;
    }
    
    public void setQueryType(String queryType) {
        this.queryType = queryType;
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
    
    public String getStartDate() {
        return startDate;
    }
    
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    public String getEndDate() {
        return endDate;
    }
    
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    
    public String getParameter() {
        return parameter;
    }
    
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getDetailLevel() {
        return detailLevel;
    }
    
    public void setDetailLevel(String detailLevel) {
        this.detailLevel = detailLevel;
    }
    
    public Boolean getIncludeTechnicalDetails() {
        return includeTechnicalDetails;
    }
    
    public void setIncludeTechnicalDetails(Boolean includeTechnicalDetails) {
        this.includeTechnicalDetails = includeTechnicalDetails;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return "QueryRequest{" +
                "query='" + query + '\'' +
                ", queryType='" + queryType + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", parameter='" + parameter + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}