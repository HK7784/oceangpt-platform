package com.oceangpt.service;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * DJL模型加载测试类
 * 用于验证PyTorch模型是否能够正确加载
 */
public class DjlModelLoadingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DjlModelLoadingTest.class);
    
    /**
     * 测试DJL加载PyTorch模型
     * 基于用户提供的测试代码
     */
    @Test
    public void testDjlModelLoading() {
        String modelPath = "D:/光谱流整合/best_model_DIN_heads8_drop0.2.pth";
        
        try {
            logger.info("开始测试DJL模型加载: {}", modelPath);
            
            // 检查模型文件是否存在
            if (!Files.exists(Paths.get(modelPath))) {
                logger.warn("模型文件不存在，跳过测试: {}", modelPath);
                return;
            }
            
            // 按照用户提供的代码进行测试
            Criteria<float[], float[]> criteria = Criteria.builder()
                .setTypes(float[].class, float[].class)
                .optModelUrls("file://" + modelPath)
                .optEngine("PyTorch")
                .build();
            
            try (ZooModel<float[], float[]> model = criteria.loadModel()) {
                logger.info("模型加载成功: {}", model.getName());
                
                // 测试预测功能
                try (Predictor<float[], float[]> predictor = model.newPredictor()) {
                    // 创建测试输入数据（假设模型需要10个特征）
                    float[] testInput = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f};
                    
                    logger.info("开始测试预测...");
                    float[] result = predictor.predict(testInput);
                    
                    logger.info("预测成功，输出维度: {}", result.length);
                    if (result.length > 0) {
                        logger.info("预测结果示例: {}", java.util.Arrays.toString(result));
                    }
                }
                
            }
            
            logger.info("DJL模型加载测试完成");
            
        } catch (Exception e) {
            logger.error("DJL模型加载测试失败: {}", e.getMessage(), e);
            
            // 输出详细的错误信息用于调试
            if (e.getCause() != null) {
                logger.error("根本原因: {}", e.getCause().getMessage());
            }
        }
    }
    
    /**
     * 主方法，用于独立运行测试
     */
    public static void main(String[] args) {
        try {
            logger.info("开始独立测试DJL模型加载...");
            
            String modelPath = "D:/光谱流整合/best_model_DIN_heads8_drop0.2.pth";
            
            // 检查模型文件是否存在
            if (!Files.exists(Paths.get(modelPath))) {
                logger.error("模型文件不存在: {}", modelPath);
                return;
            }
            
            // 按照用户提供的示例代码
            Criteria<float[], float[]> criteria = Criteria.builder()
                .setTypes(float[].class, float[].class)
                .optModelUrls("file://" + modelPath)
                .optEngine("PyTorch")
                .build();
            
            ZooModel<float[], float[]> model = criteria.loadModel();
            System.out.println("模型加载成功");
            
            model.close();
            
        } catch (Exception e) {
            logger.error("模型加载失败: {}", e.getMessage(), e);
            System.err.println("模型加载失败: " + e.getMessage());
        }
    }
}