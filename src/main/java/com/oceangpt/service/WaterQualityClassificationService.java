package com.oceangpt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 水质等级分类服务
 * 根据海水水质标准（GB 3097-1997）对水质进行分级
 */
@Service
public class WaterQualityClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WaterQualityClassificationService.class);
    
    /**
     * 水质等级枚举
     */
    public enum WaterQualityGrade {
        GRADE_1("一级", "#00FF00", "优秀", "适用于海洋渔业水域，海上天然浴场"),
        GRADE_2("二级", "#7FFF00", "良好", "适用于水产养殖区，海水浴场"),
        GRADE_3("三级", "#FFFF00", "一般", "适用于一般工业用水区，滨海风景旅游区"),
        GRADE_4("四级", "#FFA500", "较差", "适用于海洋港口水域，海洋开发作业区"),
        GRADE_INFERIOR("劣四级", "#FF0000", "极差", "超过四级标准，需要治理");
        
        private final String name;
        private final String color;
        private final String description;
        private final String usage;
        
        WaterQualityGrade(String name, String color, String description, String usage) {
            this.name = name;
            this.color = color;
            this.description = description;
            this.usage = usage;
        }
        
        public String getName() { return name; }
        public String getColor() { return color; }
        public String getDescription() { return description; }
        public String getUsage() { return usage; }
    }
    
    /**
     * 水质分类结果
     */
    public static class WaterQualityClassification {
        private WaterQualityGrade grade;
        private String reason;
        private Double overallScore;
        
        public WaterQualityClassification(WaterQualityGrade grade, String reason, Double overallScore) {
            this.grade = grade;
            this.reason = reason;
            this.overallScore = overallScore;
        }
        
        public WaterQualityGrade getGrade() { return grade; }
        public String getReason() { return reason; }
        public Double getOverallScore() { return overallScore; }
    }
    
    /**
     * 根据预测结果分类水质等级
     * 
     * @param dinLevel DIN浓度 (mg/L)
     * @param srpLevel SRP浓度 (mg/L)
     * @param phLevel pH值
     * @return 水质分类结果
     */
    public WaterQualityClassification classifyWaterQuality(Double dinLevel, Double srpLevel, Double phLevel) {
        try {
            logger.debug("开始水质分类: DIN={}, SRP={}, pH={}", dinLevel, srpLevel, phLevel);
            
            // 计算各指标的等级
            WaterQualityGrade dinGrade = classifyByDIN(dinLevel);
            WaterQualityGrade srpGrade = classifyBySRP(srpLevel);
            WaterQualityGrade phGrade = classifyByPH(phLevel);
            
            // 取最差等级作为综合等级（木桶效应）
            WaterQualityGrade overallGrade = getWorstGrade(dinGrade, srpGrade, phGrade);
            
            // 计算综合评分 (0-100)
            Double overallScore = calculateOverallScore(dinLevel, srpLevel, phLevel);
            
            // 生成分类原因
            String reason = generateClassificationReason(dinGrade, srpGrade, phGrade, overallGrade);
            
            logger.info("水质分类完成: 等级={}, 评分={}", overallGrade.getName(), overallScore);
            
            return new WaterQualityClassification(overallGrade, reason, overallScore);
            
        } catch (Exception e) {
            logger.error("水质分类失败", e);
            return new WaterQualityClassification(WaterQualityGrade.GRADE_3, "分类过程出现异常", 50.0);
        }
    }
    
    /**
     * 根据DIN浓度分类
     */
    private WaterQualityGrade classifyByDIN(Double dinLevel) {
        if (dinLevel == null) return WaterQualityGrade.GRADE_3;
        
        // 基于海水水质标准的DIN限值 (mg/L)
        if (dinLevel <= 0.20) return WaterQualityGrade.GRADE_1;      // 一级: ≤0.20
        else if (dinLevel <= 0.30) return WaterQualityGrade.GRADE_2; // 二级: ≤0.30
        else if (dinLevel <= 0.40) return WaterQualityGrade.GRADE_3; // 三级: ≤0.40
        else if (dinLevel <= 0.50) return WaterQualityGrade.GRADE_4; // 四级: ≤0.50
        else return WaterQualityGrade.GRADE_INFERIOR;                // 劣四级: >0.50
    }
    
    /**
     * 根据SRP浓度分类
     */
    private WaterQualityGrade classifyBySRP(Double srpLevel) {
        if (srpLevel == null) return WaterQualityGrade.GRADE_3;
        
        // 基于海水水质标准的SRP限值 (mg/L)
        if (srpLevel <= 0.015) return WaterQualityGrade.GRADE_1;      // 一级: ≤0.015
        else if (srpLevel <= 0.030) return WaterQualityGrade.GRADE_2; // 二级: ≤0.030
        else if (srpLevel <= 0.045) return WaterQualityGrade.GRADE_3; // 三级: ≤0.045
        else if (srpLevel <= 0.060) return WaterQualityGrade.GRADE_4; // 四级: ≤0.060
        else return WaterQualityGrade.GRADE_INFERIOR;                 // 劣四级: >0.060
    }
    
    /**
     * 根据pH值分类
     */
    private WaterQualityGrade classifyByPH(Double phLevel) {
        if (phLevel == null) return WaterQualityGrade.GRADE_3;
        
        // 基于海水水质标准的pH范围
        if (phLevel >= 7.8 && phLevel <= 8.5) return WaterQualityGrade.GRADE_1;      // 一级: 7.8-8.5
        else if (phLevel >= 7.6 && phLevel <= 8.8) return WaterQualityGrade.GRADE_2; // 二级: 7.6-8.8
        else if (phLevel >= 7.4 && phLevel <= 9.0) return WaterQualityGrade.GRADE_3; // 三级: 7.4-9.0
        else if (phLevel >= 7.0 && phLevel <= 9.5) return WaterQualityGrade.GRADE_4; // 四级: 7.0-9.5
        else return WaterQualityGrade.GRADE_INFERIOR;                                 // 劣四级: 其他
    }
    
    /**
     * 获取最差等级
     */
    private WaterQualityGrade getWorstGrade(WaterQualityGrade... grades) {
        WaterQualityGrade worst = WaterQualityGrade.GRADE_1;
        for (WaterQualityGrade grade : grades) {
            if (grade != null && grade.ordinal() > worst.ordinal()) {
                worst = grade;
            }
        }
        return worst;
    }
    
    /**
     * 计算综合评分
     */
    private Double calculateOverallScore(Double dinLevel, Double srpLevel, Double phLevel) {
        double totalScore = 0.0;
        int validCount = 0;
        
        // DIN评分 (权重: 40%)
        if (dinLevel != null) {
            double dinScore = calculateDINScore(dinLevel);
            totalScore += dinScore * 0.4;
            validCount++;
        }
        
        // SRP评分 (权重: 40%)
        if (srpLevel != null) {
            double srpScore = calculateSRPScore(srpLevel);
            totalScore += srpScore * 0.4;
            validCount++;
        }
        
        // pH评分 (权重: 20%)
        if (phLevel != null) {
            double phScore = calculatePHScore(phLevel);
            totalScore += phScore * 0.2;
            validCount++;
        }
        
        return validCount > 0 ? totalScore / (validCount * 0.2 + 0.8) * 100 : 50.0;
    }
    
    private double calculateDINScore(Double dinLevel) {
        if (dinLevel <= 0.20) return 1.0;
        else if (dinLevel <= 0.30) return 0.8;
        else if (dinLevel <= 0.40) return 0.6;
        else if (dinLevel <= 0.50) return 0.4;
        else return 0.2;
    }
    
    private double calculateSRPScore(Double srpLevel) {
        if (srpLevel <= 0.015) return 1.0;
        else if (srpLevel <= 0.030) return 0.8;
        else if (srpLevel <= 0.045) return 0.6;
        else if (srpLevel <= 0.060) return 0.4;
        else return 0.2;
    }
    
    private double calculatePHScore(Double phLevel) {
        if (phLevel >= 7.8 && phLevel <= 8.5) return 1.0;
        else if (phLevel >= 7.6 && phLevel <= 8.8) return 0.8;
        else if (phLevel >= 7.4 && phLevel <= 9.0) return 0.6;
        else if (phLevel >= 7.0 && phLevel <= 9.5) return 0.4;
        else return 0.2;
    }
    
    /**
     * 生成分类原因说明
     */
    private String generateClassificationReason(WaterQualityGrade dinGrade, WaterQualityGrade srpGrade, 
                                               WaterQualityGrade phGrade, WaterQualityGrade overallGrade) {
        StringBuilder reason = new StringBuilder();
        reason.append("综合评定为").append(overallGrade.getName()).append("水质。");
        
        if (dinGrade != null) {
            reason.append(" DIN指标: ").append(dinGrade.getName()).append(";");
        }
        if (srpGrade != null) {
            reason.append(" SRP指标: ").append(srpGrade.getName()).append(";");
        }
        if (phGrade != null) {
            reason.append(" pH指标: ").append(phGrade.getName()).append(";");
        }
        
        reason.append(" 适用范围: ").append(overallGrade.getUsage());
        
        return reason.toString();
    }
}