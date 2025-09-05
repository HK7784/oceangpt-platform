package com.oceangpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceangpt.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 趋势分析请求DTO
 * 用于接收月度趋势分析的请求参数
 */
@Schema(description = "月度趋势分析请求参数")
@ValidDateRange(startDateField = "startDate", endDateField = "endDate", message = "开始日期不能晚于结束日期")
public class TrendAnalysisRequest {
    
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
    
    @Schema(description = "开始日期", example = "2024-01-01", required = true)
    @NotNull(message = "开始日期不能为空")
    @PastOrPresent(message = "开始日期不能是未来日期")
    @JsonProperty("startDate")
    private LocalDate startDate;
    
    @Schema(description = "结束日期", example = "2024-12-31", required = true)
    @NotNull(message = "结束日期不能为空")
    @PastOrPresent(message = "结束日期不能是未来日期")
    @JsonProperty("endDate")
    private LocalDate endDate;
    
    @Schema(description = "分析参数列表", example = "[\"temperature\", \"salinity\", \"ph\"]")
    @NotEmpty(message = "分析参数列表不能为空")
    @Size(min = 1, max = 10, message = "分析参数数量必须在1到10之间")
    @JsonProperty("parameters")
    private List<String> parameters;
    
    @Schema(description = "聚合类型", example = "monthly", allowableValues = {"daily", "weekly", "monthly"})
    @Pattern(regexp = "^(daily|weekly|monthly)$", message = "聚合类型只能是daily、weekly或monthly")
    @JsonProperty("aggregationType")
    private String aggregationType = "monthly";
    
    @Schema(description = "是否包含预测", example = "true")
    @JsonProperty("includeForecast")
    private Boolean includeForecast = false;
    
    @Schema(description = "预测天数", example = "30", minimum = "0", maximum = "365")
    @Min(value = 0, message = "预测天数不能为负数")
    @Max(value = 365, message = "预测天数不能超过365天")
    @JsonProperty("forecastDays")
    private Integer forecastDays = 30;
    
    // 默认构造函数
    public TrendAnalysisRequest() {}
    
    // 构造函数
    public TrendAnalysisRequest(Double latitude, Double longitude, LocalDate startDate, LocalDate endDate) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // 全参构造函数
    public TrendAnalysisRequest(Double latitude, Double longitude, LocalDate startDate, LocalDate endDate,
                              List<String> parameters, String aggregationType, Boolean includeForecast, Integer forecastDays) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
        this.parameters = parameters;
        this.aggregationType = aggregationType;
        this.includeForecast = includeForecast;
        this.forecastDays = forecastDays;
    }
    
    // Getter和Setter方法
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
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public List<String> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
    
    public String getAggregationType() {
        return aggregationType;
    }
    
    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }
    
    public Boolean getIncludeForecast() {
        return includeForecast;
    }
    
    public void setIncludeForecast(Boolean includeForecast) {
        this.includeForecast = includeForecast;
    }
    
    public Integer getForecastDays() {
        return forecastDays;
    }
    
    public void setForecastDays(Integer forecastDays) {
        this.forecastDays = forecastDays;
    }
    
    @Override
    public String toString() {
        return "TrendAnalysisRequest{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", parameters=" + parameters +
                ", aggregationType='" + aggregationType + '\'' +
                ", includeForecast=" + includeForecast +
                ", forecastDays=" + forecastDays +
                '}';
    }
}