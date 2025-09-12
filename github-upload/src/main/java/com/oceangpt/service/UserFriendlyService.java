package com.oceangpt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户友好服务
 * 提供错误处理、参数解释和用户指导功能
 */
@Service
public class UserFriendlyService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserFriendlyService.class);
    
    // 常见错误模式
    private static final Map<String, String> ERROR_PATTERNS = new HashMap<>();
    private static final Map<String, String> PARAMETER_EXPLANATIONS = new HashMap<>();
    private static final Map<String, String> UNIT_EXPLANATIONS = new HashMap<>();
    
    static {
        // 初始化错误模式和解释
        initializeErrorPatterns();
        initializeParameterExplanations();
        initializeUnitExplanations();
    }
    
    /**
     * 初始化错误模式
     */
    private static void initializeErrorPatterns() {
        ERROR_PATTERNS.put("coordinate_invalid", "坐标格式不正确。请使用格式：经度 120.5, 纬度 36.2");
        ERROR_PATTERNS.put("coordinate_out_of_range", "坐标超出有效范围。经度范围：-180°到180°，纬度范围：-90°到90°");
        ERROR_PATTERNS.put("date_invalid", "日期格式不正确。请使用格式：2024年1月 或 2024-01-15");
        ERROR_PATTERNS.put("satellite_data_unavailable", "该位置或时间的卫星数据暂时不可用，已使用模拟数据进行预测");
        ERROR_PATTERNS.put("model_prediction_failed", "预测模型暂时不可用，请稍后重试");
        ERROR_PATTERNS.put("network_timeout", "网络连接超时，请检查网络连接后重试");
        ERROR_PATTERNS.put("insufficient_data", "提供的信息不足，请补充完整的地理坐标和时间信息");
    }
    
    /**
     * 初始化参数解释
     */
    private static void initializeParameterExplanations() {
        PARAMETER_EXPLANATIONS.put("DIN", "溶解无机氮 (Dissolved Inorganic Nitrogen)\n" +
            "• 包括硝酸盐、亚硝酸盐和铵盐\n" +
            "• 是浮游植物生长的重要营养元素\n" +
            "• 浓度过高可能导致水体富营养化");
            
        PARAMETER_EXPLANATIONS.put("SRP", "可溶性活性磷 (Soluble Reactive Phosphorus)\n" +
            "• 水中可直接被植物利用的磷形式\n" +
            "• 是限制浮游植物生长的关键因子\n" +
            "• 浓度升高可能引起藻类大量繁殖");
            
        PARAMETER_EXPLANATIONS.put("pH", "酸碱度\n" +
            "• 表示水体的酸性或碱性程度\n" +
            "• 正常海水pH值约为8.1-8.3\n" +
            "• 影响海洋生物的生存和生长");
            
        PARAMETER_EXPLANATIONS.put("Chl-a", "叶绿素a浓度\n" +
            "• 反映浮游植物的生物量\n" +
            "• 是评估水体初级生产力的重要指标\n" +
            "• 可通过卫星遥感监测");
            
        PARAMETER_EXPLANATIONS.put("TSM", "总悬浮物 (Total Suspended Matter)\n" +
            "• 水中悬浮的固体颗粒物质\n" +
            "• 影响水体透明度和光照条件\n" +
            "• 包括有机和无机颗粒");
    }
    
    /**
     * 初始化单位解释
     */
    private static void initializeUnitExplanations() {
        UNIT_EXPLANATIONS.put("mg/L", "毫克每升 - 表示每升水中含有多少毫克的物质");
        UNIT_EXPLANATIONS.put("μg/L", "微克每升 - 表示每升水中含有多少微克的物质（1毫克=1000微克）");
        UNIT_EXPLANATIONS.put("μmol/L", "微摩尔每升 - 表示每升水中含有多少微摩尔的物质");
        UNIT_EXPLANATIONS.put("°", "度 - 地理坐标的角度单位");
        UNIT_EXPLANATIONS.put("%", "百分比 - 表示比例或置信度");
    }
    
    /**
     * 获取友好的错误消息
     * 
     * @param errorCode 错误代码
     * @param originalError 原始错误信息
     * @return 用户友好的错误消息
     */
    public String getFriendlyErrorMessage(String errorCode, String originalError) {
        logger.debug("生成友好错误消息: errorCode={}, originalError={}", errorCode, originalError);
        
        String friendlyMessage = ERROR_PATTERNS.get(errorCode);
        if (friendlyMessage != null) {
            return "❌ " + friendlyMessage + "\n\n💡 如需帮助，请输入 '帮助' 获取使用指南。";
        }
        
        // 如果没有预定义的友好消息，尝试解析原始错误
        return generateFriendlyMessageFromError(originalError);
    }
    
    /**
     * 从原始错误生成友好消息
     */
    private String generateFriendlyMessageFromError(String originalError) {
        if (originalError == null || originalError.isEmpty()) {
            return "❌ 发生了未知错误，请稍后重试。";
        }
        
        String lowerError = originalError.toLowerCase();
        
        if (lowerError.contains("timeout") || lowerError.contains("connection")) {
            return getFriendlyErrorMessage("network_timeout", null);
        } else if (lowerError.contains("coordinate") || lowerError.contains("latitude") || lowerError.contains("longitude")) {
            return getFriendlyErrorMessage("coordinate_invalid", null);
        } else if (lowerError.contains("date") || lowerError.contains("time")) {
            return getFriendlyErrorMessage("date_invalid", null);
        } else if (lowerError.contains("satellite") || lowerError.contains("data")) {
            return getFriendlyErrorMessage("satellite_data_unavailable", null);
        } else {
            return "❌ " + originalError + "\n\n💡 如需帮助，请输入 '帮助' 获取使用指南。";
        }
    }
    
    /**
     * 获取参数解释
     * 
     * @param parameter 参数名称
     * @return 参数解释
     */
    public String getParameterExplanation(String parameter) {
        String explanation = PARAMETER_EXPLANATIONS.get(parameter.toUpperCase());
        if (explanation != null) {
            return "📖 **" + parameter + "参数说明**\n\n" + explanation;
        }
        return "参数 '" + parameter + "' 的详细说明暂时不可用。";
    }
    
    /**
     * 获取单位解释
     * 
     * @param unit 单位
     * @return 单位解释
     */
    public String getUnitExplanation(String unit) {
        String explanation = UNIT_EXPLANATIONS.get(unit);
        if (explanation != null) {
            return "📏 **单位说明**: " + explanation;
        }
        return "单位 '" + unit + "' 的说明暂时不可用。";
    }
    
    /**
     * 生成使用指南
     * 
     * @return 使用指南
     */
    public String generateUsageGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("🌊 **OceanGPT 使用指南**\n\n");
        
        guide.append("📍 **如何进行水质预测**:\n");
        guide.append("• 提供地理坐标：\"经纬度 120.5, 36.2\"\n");
        guide.append("• 指定时间（可选）：\"2024年1月\" 或 \"2024-01-15\"\n");
        guide.append("• 示例：\"预测经纬度 120.5, 36.2 在2024年1月的水质\"\n\n");
        
        guide.append("🔬 **可预测的水质参数**:\n");
        guide.append("• DIN - 溶解无机氮 (mg/L)\n");
        guide.append("• SRP - 可溶性活性磷 (mg/L)\n");
        guide.append("• pH - 酸碱度\n\n");
        
        guide.append("📊 **数据来源**:\n");
        guide.append("• Sentinel-2/3 卫星遥感数据\n");
        guide.append("• EndToEndRegressionModel 机器学习模型\n");
        guide.append("• 历史海洋观测数据\n\n");
        
        guide.append("💡 **使用技巧**:\n");
        guide.append("• 输入 '参数说明 DIN' 了解参数详情\n");
        guide.append("• 输入 '单位说明 mg/L' 了解单位含义\n");
        guide.append("• 支持中文和英文查询\n");
        guide.append("• 可以询问水质趋势、污染源分析等问题\n\n");
        
        guide.append("❓ **常见问题**:\n");
        guide.append("• 坐标格式：经度在前，纬度在后\n");
        guide.append("• 坐标范围：经度 -180°~180°，纬度 -90°~90°\n");
        guide.append("• 预测精度：基于历史数据训练，置信度通常在80-95%\n");
        
        return guide.toString();
    }
    
    /**
     * 验证坐标格式
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @return 验证结果和错误信息
     */
    public ValidationResult validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return new ValidationResult(false, "coordinate_invalid", "缺少坐标信息");
        }
        
        if (latitude < -90 || latitude > 90) {
            return new ValidationResult(false, "coordinate_out_of_range", 
                String.format("纬度 %.4f° 超出有效范围 (-90° 到 90°)", latitude));
        }
        
        if (longitude < -180 || longitude > 180) {
            return new ValidationResult(false, "coordinate_out_of_range", 
                String.format("经度 %.4f° 超出有效范围 (-180° 到 180°)", longitude));
        }
        
        return new ValidationResult(true, null, "坐标验证通过");
    }
    
    /**
     * 检查用户输入是否为帮助请求
     * 
     * @param message 用户消息
     * @return 是否为帮助请求
     */
    public boolean isHelpRequest(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase().trim();
        return lowerMessage.equals("帮助") || lowerMessage.equals("help") ||
               lowerMessage.equals("使用指南") || lowerMessage.equals("guide") ||
               lowerMessage.contains("怎么用") || lowerMessage.contains("如何使用");
    }
    
    /**
     * 检查用户输入是否为参数解释请求
     * 
     * @param message 用户消息
     * @return 参数名称，如果不是参数解释请求则返回null
     */
    public String extractParameterExplanationRequest(String message) {
        if (message == null) return null;
        
        String lowerMessage = message.toLowerCase();
        
        // 匹配 "参数说明 DIN" 或 "DIN是什么" 等格式
        if (lowerMessage.contains("参数说明") || lowerMessage.contains("参数解释")) {
            for (String param : PARAMETER_EXPLANATIONS.keySet()) {
                if (lowerMessage.contains(param.toLowerCase())) {
                    return param;
                }
            }
        }
        
        // 匹配 "什么是DIN" 或 "DIN的含义" 等格式
        for (String param : PARAMETER_EXPLANATIONS.keySet()) {
            if (lowerMessage.contains(param.toLowerCase()) && 
                (lowerMessage.contains("是什么") || lowerMessage.contains("含义") || 
                 lowerMessage.contains("意思") || lowerMessage.contains("解释"))) {
                return param;
            }
        }
        
        return null;
    }
    
    /**
     * 检查用户输入是否为单位解释请求
     * 
     * @param message 用户消息
     * @return 单位名称，如果不是单位解释请求则返回null
     */
    public String extractUnitExplanationRequest(String message) {
        if (message == null) return null;
        
        String lowerMessage = message.toLowerCase();
        
        // 匹配 "单位说明 mg/L" 等格式
        if (lowerMessage.contains("单位说明") || lowerMessage.contains("单位解释")) {
            for (String unit : UNIT_EXPLANATIONS.keySet()) {
                if (lowerMessage.contains(unit.toLowerCase())) {
                    return unit;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 生成数据质量说明
     * 
     * @param qualityScore 质量评分 (0-1)
     * @param dataSource 数据源
     * @return 数据质量说明
     */
    public String generateDataQualityExplanation(Double qualityScore, String dataSource) {
        if (qualityScore == null) {
            return "📊 数据质量信息不可用";
        }
        
        StringBuilder explanation = new StringBuilder();
        explanation.append("📊 **数据质量评估**\n");
        explanation.append(String.format("• 质量评分: %.1f%%\n", qualityScore * 100));
        
        if (qualityScore >= 0.9) {
            explanation.append("• 质量等级: 优秀 🌟\n");
            explanation.append("• 说明: 数据完整性高，预测结果可靠\n");
        } else if (qualityScore >= 0.7) {
            explanation.append("• 质量等级: 良好 ✅\n");
            explanation.append("• 说明: 数据质量较好，预测结果基本可靠\n");
        } else if (qualityScore >= 0.5) {
            explanation.append("• 质量等级: 中等 ⚠️\n");
            explanation.append("• 说明: 数据质量一般，预测结果仅供参考\n");
        } else {
            explanation.append("• 质量等级: 较差 ❌\n");
            explanation.append("• 说明: 数据质量较差，建议谨慎使用预测结果\n");
        }
        
        if (dataSource != null) {
            explanation.append(String.format("• 数据源: %s\n", dataSource));
        }
        
        return explanation.toString();
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String message;
        
        public ValidationResult(boolean valid, String errorCode, String message) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public String getMessage() {
            return message;
        }
    }
}