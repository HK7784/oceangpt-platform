package com.oceangpt.service;

import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.SatelliteDataRequest;
import com.oceangpt.dto.SatelliteDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 参数映射服务
 * 将用户友好的输入（经纬度、时间）转换为模型所需的专业参数
 */
@Service
public class ParameterMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ParameterMappingService.class);
    
    @Autowired
    private SatelliteDataService satelliteDataService;
    
    @Autowired
    private DataInterpolationService dataInterpolationService;
    
    /**
     * 将用户输入转换为预测请求
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param dateTime 时间（可选）
     * @param additionalParams 额外参数
     * @return 完整的预测请求
     */
    public PredictionRequest mapUserInputToPredictionRequest(Double latitude, Double longitude, 
                                                           LocalDateTime dateTime, 
                                                           Map<String, Object> additionalParams) {
        try {
            logger.info("开始参数映射: 经度={}, 纬度={}, 时间={}", longitude, latitude, dateTime);
            
            // 创建预测请求
            PredictionRequest request = new PredictionRequest();
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            
            // 设置时间，如果未提供则使用当前时间
            if (dateTime == null) {
                dateTime = LocalDateTime.now();
            }
            request.setDateTime(dateTime);
            
            // 使用智能数据插值服务获取卫星数据
            SatelliteDataResponse satelliteData = dataInterpolationService.getInterpolatedSatelliteData(
                latitude, longitude, dateTime);
            
            if (satelliteData != null && satelliteData.isSuccess()) {
                logger.info("成功获取卫星数据，数据源: {}, 质量评分: {}", 
                    satelliteData.getDataSource(), satelliteData.getQualityScore());
                
                // 映射Sentinel-2数据
                mapSentinel2Data(request, satelliteData);
                
                // 映射Sentinel-3数据
                mapSentinel3Data(request, satelliteData);
                
                // 设置神经网络预测值
                request.setChlNN(satelliteData.getChlNN());
                request.setTsmNN(satelliteData.getTsmNN());
                
                logger.info("卫星数据映射成功");
            } else {
                logger.warn("卫星数据获取失败，使用默认值: {}", 
                    satelliteData != null ? satelliteData.getMessage() : "未知错误");
                // 使用默认值或历史平均值
                setDefaultSpectralValues(request, latitude, longitude);
            }
            
            // 处理额外参数
            if (additionalParams != null) {
                processAdditionalParameters(request, additionalParams);
            }
            
            logger.info("参数映射完成");
            return request;
            
        } catch (Exception e) {
            logger.error("参数映射失败: {}", e.getMessage(), e);
            throw new RuntimeException("参数映射失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将用户输入映射为参数
     * 
     * @param userInput 用户输入的自然语言
     * @return 包含坐标、时间和卫星数据的参数映射
     */
    public Map<String, Object> mapUserInputToParameters(String userInput) {
        logger.info("开始解析用户输入: {}", userInput);
        
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            // 提取基础参数
            Map<String, Object> extractedParams = extractParametersFromNaturalLanguage(userInput);
            parameters.putAll(extractedParams);
            
            // 如果提取到了坐标信息，获取卫星数据
            if (parameters.containsKey("latitude") && parameters.containsKey("longitude")) {
                Double latitude = (Double) parameters.get("latitude");
                Double longitude = (Double) parameters.get("longitude");
                LocalDateTime dateTime = (LocalDateTime) parameters.get("dateTime");
                
                if (dateTime == null) {
                    dateTime = LocalDateTime.now();
                    parameters.put("dateTime", dateTime);
                }
                
                // 获取卫星数据
                SatelliteDataResponse satelliteData = satelliteDataService.getSatelliteData(
                    latitude, longitude, dateTime);
                
                if (satelliteData != null && satelliteData.isSuccess()) {
                    parameters.put("s2Data", satelliteData.getS2Data());
                    parameters.put("s3Data", satelliteData.getS3Data());
                    parameters.put("chlNN", satelliteData.getChlNN());
                    parameters.put("tsmNN", satelliteData.getTsmNN());
                    parameters.put("dataSource", satelliteData.getDataSource());
                    parameters.put("qualityScore", satelliteData.getQualityScore());
                    logger.info("成功获取卫星数据，数据源: {}", satelliteData.getDataSource());
                } else {
                    // 使用默认值
                    parameters.put("s2Data", getDefaultSentinel2Data());
                    parameters.put("s3Data", getDefaultSentinel3Data());
                    parameters.put("chlNN", 0.5);
                    parameters.put("tsmNN", 1.0);
                    parameters.put("dataSource", "默认值");
                    parameters.put("qualityScore", 0.5);
                    logger.warn("卫星数据获取失败，使用默认值");
                }
            } else {
                logger.warn("未能从用户输入中提取到有效坐标: {}", userInput);
            }
            
        } catch (Exception e) {
            logger.error("用户输入解析失败: {}", e.getMessage(), e);
        }
        
        logger.info("用户输入解析完成，提取到参数: {}", parameters.keySet());
        return parameters;
    }
    
    /**
     * 从自然语言中提取地理和时间信息
     * 
     * @param naturalLanguageQuery 自然语言查询
     * @return 提取的参数映射
     */
    public Map<String, Object> extractParametersFromNaturalLanguage(String naturalLanguageQuery) {
        Map<String, Object> extractedParams = new HashMap<>();
        
        try {
            // 提取经纬度信息
            Double[] coordinates = extractCoordinates(naturalLanguageQuery);
            if (coordinates != null) {
                extractedParams.put("latitude", coordinates[0]);
                extractedParams.put("longitude", coordinates[1]);
            }
            
            // 提取时间信息
            LocalDateTime dateTime = extractDateTime(naturalLanguageQuery);
            if (dateTime != null) {
                extractedParams.put("dateTime", dateTime);
            }
            
            // 提取水质参数关注点
            List<String> parameters = extractWaterQualityParameters(naturalLanguageQuery);
            if (!parameters.isEmpty()) {
                extractedParams.put("focusParameters", parameters);
            }
            
            // 提取查询类型
            String queryType = extractQueryType(naturalLanguageQuery);
            extractedParams.put("queryType", queryType);
            
            logger.info("从自然语言中提取参数: {}", extractedParams);
            
        } catch (Exception e) {
            logger.error("自然语言参数提取失败: {}", e.getMessage(), e);
        }
        
        return extractedParams;
    }
    
    /**
     * 映射Sentinel-2光谱数据
     */
    private void mapSentinel2Data(PredictionRequest request, SatelliteDataResponse satelliteData) {
        if (satelliteData.getS2Data() != null) {
            Map<String, Double> s2Data = satelliteData.getS2Data();
            request.setS2B2(s2Data.get("B2"));
            request.setS2B3(s2Data.get("B3"));
            request.setS2B4(s2Data.get("B4"));
            request.setS2B5(s2Data.get("B5"));
            request.setS2B6(s2Data.get("B6"));
            request.setS2B7(s2Data.get("B7"));
            request.setS2B8(s2Data.get("B8"));
            request.setS2B8A(s2Data.get("B8A"));
        }
    }
    
    /**
     * 映射Sentinel-3光谱数据
     */
    private void mapSentinel3Data(PredictionRequest request, SatelliteDataResponse satelliteData) {
        if (satelliteData.getS3Data() != null) {
            Map<String, Double> s3Data = satelliteData.getS3Data();
            request.setS3Oa01(s3Data.get("Oa01"));
            request.setS3Oa02(s3Data.get("Oa02"));
            request.setS3Oa03(s3Data.get("Oa03"));
            request.setS3Oa04(s3Data.get("Oa04"));
            request.setS3Oa05(s3Data.get("Oa05"));
            request.setS3Oa06(s3Data.get("Oa06"));
            request.setS3Oa07(s3Data.get("Oa07"));
            request.setS3Oa08(s3Data.get("Oa08"));
        }
    }
    
    /**
     * 设置默认光谱值（基于地理位置的历史平均值）
     */
    private void setDefaultSpectralValues(PredictionRequest request, Double latitude, Double longitude) {
        // 基于地理位置设置合理的默认值
        // 这里可以根据历史数据或地理特征来设置更准确的默认值
        
        // Sentinel-2默认值（基于典型海洋光谱特征）
        request.setS2B2(0.05);  // 蓝光波段
        request.setS2B3(0.04);  // 绿光波段
        request.setS2B4(0.03);  // 红光波段
        request.setS2B5(0.025); // 红边波段
        request.setS2B6(0.02);  // 红边波段
        request.setS2B7(0.015); // 红边波段
        request.setS2B8(0.01);  // 近红外波段
        request.setS2B8A(0.012); // 近红外波段
        
        // Sentinel-3默认值
        request.setS3Oa01(0.045);
        request.setS3Oa02(0.04);
        request.setS3Oa03(0.035);
        request.setS3Oa04(0.03);
        request.setS3Oa05(0.025);
        request.setS3Oa06(0.02);
        request.setS3Oa07(0.018);
        request.setS3Oa08(0.015);
        
        // 神经网络预测值默认值
        request.setChlNN(0.5);  // 叶绿素浓度
        request.setTsmNN(1.0);  // 总悬浮物浓度
        
        logger.info("已设置默认光谱值");
    }
    
    /**
     * 获取默认的Sentinel-2数据
     */
    private Map<String, Double> getDefaultSentinel2Data() {
        Map<String, Double> s2Data = new HashMap<>();
        s2Data.put("B2", 0.05);   // 蓝光波段
        s2Data.put("B3", 0.04);   // 绿光波段
        s2Data.put("B4", 0.03);   // 红光波段
        s2Data.put("B5", 0.025);  // 红边波段
        s2Data.put("B6", 0.02);   // 红边波段
        s2Data.put("B7", 0.015);  // 红边波段
        s2Data.put("B8", 0.01);   // 近红外波段
        s2Data.put("B8A", 0.012); // 近红外波段
        return s2Data;
    }
    
    /**
     * 获取默认的Sentinel-3数据
     */
    private Map<String, Double> getDefaultSentinel3Data() {
        Map<String, Double> s3Data = new HashMap<>();
        s3Data.put("Oa01", 0.045);
        s3Data.put("Oa02", 0.04);
        s3Data.put("Oa03", 0.035);
        s3Data.put("Oa04", 0.03);
        s3Data.put("Oa05", 0.025);
        s3Data.put("Oa06", 0.02);
        s3Data.put("Oa07", 0.018);
        s3Data.put("Oa08", 0.015);
        return s3Data;
    }
    
    /**
     * 从自然语言中提取坐标
     */
    private Double[] extractCoordinates(String query) {
        try {
            // 首先尝试从地名获取坐标
            Double[] namedLocation = extractCoordinatesFromLocationName(query);
            if (namedLocation != null) {
                return namedLocation;
            }
            
            // 匹配"经度 120.5, 纬度 36.2"格式
            java.util.regex.Pattern lonLatPattern = java.util.regex.Pattern.compile(
                "(?:经度|longitude)\\s*([+-]?\\d+(?:\\.\\d+)?).*?(?:纬度|latitude)\\s*([+-]?\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher lonLatMatcher = lonLatPattern.matcher(query);
            
            if (lonLatMatcher.find()) {
                double longitude = Double.parseDouble(lonLatMatcher.group(1));
                double latitude = Double.parseDouble(lonLatMatcher.group(2));
                
                // 验证坐标范围
                if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                    logger.info("提取到坐标: 经度={}, 纬度={}", longitude, latitude);
                    return new Double[]{latitude, longitude};
                }
            }
            
            // 匹配"纬度 36.2, 经度 120.5"格式（顺序相反）
            java.util.regex.Pattern latLonPattern = java.util.regex.Pattern.compile(
                "(?:纬度|latitude)\\s*([+-]?\\d+(?:\\.\\d+)?).*?(?:经度|longitude)\\s*([+-]?\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher latLonMatcher = latLonPattern.matcher(query);
            
            if (latLonMatcher.find()) {
                double latitude = Double.parseDouble(latLonMatcher.group(1));
                double longitude = Double.parseDouble(latLonMatcher.group(2));
                
                // 验证坐标范围
                if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                    logger.info("提取到坐标: 纬度={}, 经度={}", latitude, longitude);
                    return new Double[]{latitude, longitude};
                }
            }
            
            // 匹配经纬度模式："经纬度 120，36" 或 "120,36" 等
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:经纬度|坐标|位置)?\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*[,，]\\s*([+-]?\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(query);
            
            if (matcher.find()) {
                double longitude = Double.parseDouble(matcher.group(1));
                double latitude = Double.parseDouble(matcher.group(2));
                
                // 验证坐标范围
                if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                    logger.info("提取到坐标: 经度={}, 纬度={}", longitude, latitude);
                    return new Double[]{latitude, longitude};
                }
            }
            
            // 匹配度分秒格式："120°30'E, 36°20'N"
             java.util.regex.Pattern degreePattern = java.util.regex.Pattern.compile(
                 "(\\d+)°(?:(\\d+)')?(?:(\\d+)\")?[EW]?[,，]?\\s*(\\d+)°(?:(\\d+)')?(?:(\\d+)\")?[NS]?");
            java.util.regex.Matcher degreeMatcher = degreePattern.matcher(query);
            
            if (degreeMatcher.find()) {
                double lon = Double.parseDouble(degreeMatcher.group(1));
                if (degreeMatcher.group(2) != null) {
                    lon += Double.parseDouble(degreeMatcher.group(2)) / 60.0;
                }
                if (degreeMatcher.group(3) != null) {
                    lon += Double.parseDouble(degreeMatcher.group(3)) / 3600.0;
                }
                
                double lat = Double.parseDouble(degreeMatcher.group(4));
                if (degreeMatcher.group(5) != null) {
                    lat += Double.parseDouble(degreeMatcher.group(5)) / 60.0;
                }
                if (degreeMatcher.group(6) != null) {
                    lat += Double.parseDouble(degreeMatcher.group(6)) / 3600.0;
                }
                
                // 验证坐标范围
                if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                    return new Double[]{lat, lon};
                }
            }
            
        } catch (Exception e) {
            logger.debug("坐标提取失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 从地名提取坐标
     */
    private Double[] extractCoordinatesFromLocationName(String query) {
        String lowerQuery = query.toLowerCase();
        
        // 中国主要海域和城市的坐标映射
        Map<String, Double[]> locationMap = new HashMap<>();
        
        // 海域坐标（中心点）
        locationMap.put("渤海", new Double[]{38.5, 120.0});
        locationMap.put("黄海", new Double[]{35.0, 123.0});
        locationMap.put("东海", new Double[]{28.0, 125.0});
        locationMap.put("南海", new Double[]{15.0, 115.0});
        
        // 沿海城市坐标
        locationMap.put("青岛", new Double[]{36.0671, 120.3826});
        locationMap.put("大连", new Double[]{38.9140, 121.6147});
        locationMap.put("上海", new Double[]{31.2304, 121.4737});
        locationMap.put("厦门", new Double[]{24.4798, 118.0894});
        locationMap.put("深圳", new Double[]{22.5431, 114.0579});
        locationMap.put("海南", new Double[]{19.5664, 109.9497});
        locationMap.put("舟山", new Double[]{29.9853, 122.2072});
        locationMap.put("威海", new Double[]{37.5128, 122.1201});
        locationMap.put("烟台", new Double[]{37.4638, 121.4478});
        locationMap.put("天津", new Double[]{39.0842, 117.2009});
        locationMap.put("宁波", new Double[]{29.8683, 121.5440});
        locationMap.put("福州", new Double[]{26.0745, 119.2965});
        locationMap.put("广州", new Double[]{23.1291, 113.2644});
        locationMap.put("珠海", new Double[]{22.2711, 113.5767});
        
        // 检查是否包含已知地名
        for (Map.Entry<String, Double[]> entry : locationMap.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                logger.info("识别到地名: {}, 坐标: [{}, {}]", entry.getKey(), 
                    entry.getValue()[0], entry.getValue()[1]);
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 从自然语言中提取时间信息
     */
    private LocalDateTime extractDateTime(String query) {
        try {
            String lowerQuery = query.toLowerCase();
            LocalDateTime now = LocalDateTime.now();
            
            // 匹配年月模式："2024年1月" 或 "2024-01"
            java.util.regex.Pattern yearMonthPattern = java.util.regex.Pattern.compile(
                "(\\d{4})年?(\\d{1,2})月?");
            java.util.regex.Matcher matcher = yearMonthPattern.matcher(query);
            
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                logger.info("提取到具体时间: {}年{}月", year, month);
                return LocalDateTime.of(year, month, 1, 0, 0);
            }
            
            // 匹配月份名称
            Map<String, Integer> monthMap = new HashMap<>();
            monthMap.put("一月", 1); monthMap.put("1月", 1);
            monthMap.put("二月", 2); monthMap.put("2月", 2);
            monthMap.put("三月", 3); monthMap.put("3月", 3);
            monthMap.put("四月", 4); monthMap.put("4月", 4);
            monthMap.put("五月", 5); monthMap.put("5月", 5);
            monthMap.put("六月", 6); monthMap.put("6月", 6);
            monthMap.put("七月", 7); monthMap.put("7月", 7);
            monthMap.put("八月", 8); monthMap.put("8月", 8);
            monthMap.put("九月", 9); monthMap.put("9月", 9);
            monthMap.put("十月", 10); monthMap.put("10月", 10);
            monthMap.put("十一月", 11); monthMap.put("11月", 11);
            monthMap.put("十二月", 12); monthMap.put("12月", 12);
            
            for (Map.Entry<String, Integer> entry : monthMap.entrySet()) {
                if (lowerQuery.contains(entry.getKey())) {
                    int month = entry.getValue();
                    int year = now.getYear();
                    
                    // 如果指定的月份已经过去，则使用下一年
                    if (month < now.getMonthValue()) {
                        year++;
                    }
                    
                    logger.info("提取到月份: {}, 使用年份: {}", entry.getKey(), year);
                    return LocalDateTime.of(year, month, 1, 0, 0);
                }
            }
            
            // 匹配季节
            if (lowerQuery.contains("春季") || lowerQuery.contains("春天")) {
                return LocalDateTime.of(now.getYear(), 3, 1, 0, 0);
            } else if (lowerQuery.contains("夏季") || lowerQuery.contains("夏天")) {
                return LocalDateTime.of(now.getYear(), 6, 1, 0, 0);
            } else if (lowerQuery.contains("秋季") || lowerQuery.contains("秋天")) {
                return LocalDateTime.of(now.getYear(), 9, 1, 0, 0);
            } else if (lowerQuery.contains("冬季") || lowerQuery.contains("冬天")) {
                return LocalDateTime.of(now.getYear(), 12, 1, 0, 0);
            }
            
            // 匹配相对时间
            if (lowerQuery.contains("今天") || lowerQuery.contains("现在")) {
                return now;
            } else if (lowerQuery.contains("明天")) {
                return now.plusDays(1);
            } else if (lowerQuery.contains("后天")) {
                return now.plusDays(2);
            } else if (lowerQuery.contains("未来一周") || lowerQuery.contains("下周")) {
                return now.plusWeeks(1);
            } else if (lowerQuery.contains("下个月") || lowerQuery.contains("下月")) {
                return now.plusMonths(1);
            } else if (lowerQuery.contains("明年")) {
                return now.plusYears(1);
            } else if (lowerQuery.contains("上个月") || lowerQuery.contains("上月")) {
                return now.minusMonths(1);
            } else if (lowerQuery.contains("去年")) {
                return now.minusYears(1);
            }
            
            // 匹配数字+时间单位模式："3个月后"、"2周前"
             java.util.regex.Pattern relativePattern = java.util.regex.Pattern.compile(
                 "(\\d+)\\s*(个)?(天|日|周|月|年)(前|后|内)?");
            java.util.regex.Matcher relativeMatcher = relativePattern.matcher(lowerQuery);
            
            if (relativeMatcher.find()) {
                int amount = Integer.parseInt(relativeMatcher.group(1));
                String unit = relativeMatcher.group(3);
                String direction = relativeMatcher.group(4);
                
                boolean isForward = direction == null || direction.equals("后") || direction.equals("内");
                
                switch (unit) {
                    case "天":
                    case "日":
                        return isForward ? now.plusDays(amount) : now.minusDays(amount);
                    case "周":
                        return isForward ? now.plusWeeks(amount) : now.minusWeeks(amount);
                    case "月":
                        return isForward ? now.plusMonths(amount) : now.minusMonths(amount);
                    case "年":
                        return isForward ? now.plusYears(amount) : now.minusYears(amount);
                }
            }
            
        } catch (Exception e) {
            logger.debug("时间提取失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 从自然语言中提取水质参数
     */
    private List<String> extractWaterQualityParameters(String query) {
        List<String> parameters = new java.util.ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        // DIN (溶解无机氮)
        if (lowerQuery.contains("din") || lowerQuery.contains("氮") || 
            lowerQuery.contains("营养盐") || lowerQuery.contains("无机氮") ||
            lowerQuery.contains("dissolved inorganic nitrogen")) {
            parameters.add("DIN");
        }
        
        // SRP (可溶性活性磷)
        if (lowerQuery.contains("srp") || lowerQuery.contains("磷") ||
            lowerQuery.contains("活性磷") || lowerQuery.contains("可溶性磷") ||
            lowerQuery.contains("soluble reactive phosphorus")) {
            parameters.add("SRP");
        }
        
        // pH值
        if (lowerQuery.contains("ph") || lowerQuery.contains("酸碱") ||
            lowerQuery.contains("酸度") || lowerQuery.contains("碱度")) {
            parameters.add("pH");
        }
        
        // 叶绿素
        if (lowerQuery.contains("叶绿素") || lowerQuery.contains("chl") ||
            lowerQuery.contains("chlorophyll") || lowerQuery.contains("藻类")) {
            parameters.add("CHL");
        }
        
        // 总悬浮物
        if (lowerQuery.contains("悬浮物") || lowerQuery.contains("tsm") ||
            lowerQuery.contains("total suspended matter") || lowerQuery.contains("浊度")) {
            parameters.add("TSM");
        }
        
        // 水温
        if (lowerQuery.contains("温度") || lowerQuery.contains("水温") ||
            lowerQuery.contains("temperature")) {
            parameters.add("TEMPERATURE");
        }
        
        // 盐度
        if (lowerQuery.contains("盐度") || lowerQuery.contains("salinity")) {
            parameters.add("SALINITY");
        }
        
        logger.info("提取到水质参数: {}", parameters);
        return parameters;
    }
    
    /**
     * 从自然语言中提取查询类型
     */
    private String extractQueryType(String query) {
        if (query.contains("预测") || query.contains("未来")) {
            return "PREDICTION";
        } else if (query.contains("趋势") || query.contains("变化")) {
            return "TREND";
        } else if (query.contains("为什么") || query.contains("原因")) {
            return "EXPLANATION";
        } else if (query.contains("建议") || query.contains("措施")) {
            return "RECOMMENDATION";
        } else {
            return "GENERAL";
        }
    }
    
    /**
     * 处理额外参数
     */
    private void processAdditionalParameters(PredictionRequest request, Map<String, Object> additionalParams) {
        // 处理用户提供的额外参数
        for (Map.Entry<String, Object> entry : additionalParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            switch (key.toLowerCase()) {
                case "depth":
                case "深度":
                    // 可以根据深度调整光谱衰减
                    break;
                case "season":
                case "季节":
                    // 可以根据季节调整参数
                    break;
                case "weather":
                case "天气":
                    // 可以根据天气条件调整参数
                    break;
                default:
                    logger.debug("未处理的额外参数: {} = {}", key, value);
            }
        }
    }
}