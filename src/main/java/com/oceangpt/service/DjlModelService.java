package com.oceangpt.service;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import com.oceangpt.config.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DJL模型服务类
 * 负责使用DJL加载PyTorch模型进行推理
 */
@Service
public class DjlModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(DjlModelService.class);
    
    @Autowired
    private ModelConfig modelConfig;
    
    private ZooModel<float[], float[]> model;
    private boolean modelLoaded = false;
    private final ReentrantReadWriteLock modelLock = new ReentrantReadWriteLock();
    
    /**
     * 初始化模型服务
     */
    @PostConstruct
    public void initialize() {
        try {
            logger.info("初始化DJL模型服务...");
            loadModel();
            logger.info("DJL模型服务初始化成功");
        } catch (Exception e) {
            logger.error("DJL模型服务初始化失败: {}", e.getMessage(), e);
            // 在mock模式下继续运行
            if (modelConfig.isMockMode()) {
                logger.info("运行在Mock模式下，跳过实际模型加载");
                modelLoaded = true;
            }
        }
    }
    
    /**
     * 加载PyTorch模型
     */
    private void loadModel() throws Exception {
        modelLock.writeLock().lock();
        try {
            // 检查是否为mock模式
            if (modelConfig.isMockMode()) {
                logger.info("运行在Mock模式下，跳过实际模型加载");
                modelLoaded = true;
                return;
            }
            
            String modelPath = modelConfig.getPath();
            logger.info("开始加载PyTorch模型: {}", modelPath);
            
            // 检查模型文件是否存在
            Path modelFile = Paths.get(modelPath);
            if (!Files.exists(modelFile)) {
                throw new RuntimeException("模型文件不存在: " + modelPath);
            }
            
            // 检查文件扩展名
            if (!modelPath.toLowerCase().endsWith(".pth")) {
                throw new RuntimeException("不支持的模型文件格式，请使用.pth文件: " + modelPath);
            }
            
            // 构建DJL Criteria
            Criteria<float[], float[]> criteria = Criteria.builder()
                .setTypes(float[].class, float[].class)
                .optModelUrls("file://" + modelFile.toAbsolutePath().toString())
                .optEngine("PyTorch")
                .build();
            
            // 加载模型
            long startTime = System.currentTimeMillis();
            model = criteria.loadModel();
            long loadTime = System.currentTimeMillis() - startTime;
            
            logger.info("PyTorch模型加载成功，耗时: {} ms", loadTime);
            modelLoaded = true;
            
        } finally {
            modelLock.writeLock().unlock();
        }
    }
    
    /**
     * 检查模型是否已加载
     */
    public boolean isModelReady() {
        modelLock.readLock().lock();
        try {
            return modelLoaded;
        } finally {
            modelLock.readLock().unlock();
        }
    }
    
    /**
     * 获取模型信息
     */
    public Map<String, Object> getModelInfo() {
        modelLock.readLock().lock();
        try {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("modelPath", modelConfig.getPath());
            info.put("mockMode", modelConfig.isMockMode());
            info.put("status", modelLoaded ? "已加载" : "未加载");
            info.put("engine", "PyTorch (DJL)");
            
            if (modelLoaded && model != null && !modelConfig.isMockMode()) {
                info.put("modelName", model.getName());
            }
            
            return info;
        } finally {
            modelLock.readLock().unlock();
        }
    }
    
    /**
     * 执行模型预测
     */
    public float[] predict(float[] input) throws Exception {
        modelLock.readLock().lock();
        try {
            if (!modelLoaded) {
                throw new RuntimeException("模型未加载");
            }
            
            // Mock模式返回模拟结果
            if (modelConfig.isMockMode()) {
                logger.debug("Mock模式：返回模拟预测结果");
                return new float[]{0.5f, 0.3f, 7.2f}; // DIN, SRP, pH的模拟值
            }
            
            // 使用DJL进行实际预测
            try (Predictor<float[], float[]> predictor = model.newPredictor()) {
                return predictor.predict(input);
            }
            
        } finally {
            modelLock.readLock().unlock();
        }
    }
    
    /**
     * 测试模型加载（用于验证）
     */
    public static void testModelLoading(String modelPath) {
        try {
            logger.info("测试模型加载: {}", modelPath);
            
            Criteria<float[], float[]> criteria = Criteria.builder()
                .setTypes(float[].class, float[].class)
                .optModelUrls("file://" + modelPath)
                .optEngine("PyTorch")
                .build();
            
            try (ZooModel<float[], float[]> testModel = criteria.loadModel()) {
                logger.info("模型加载成功: {}", testModel.getName());
            }
            
        } catch (Exception e) {
            logger.error("模型加载测试失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 资源清理
     */
    @PreDestroy
    public void cleanup() {
        modelLock.writeLock().lock();
        try {
            if (model != null) {
                logger.info("清理DJL模型资源");
                model.close();
                model = null;
                modelLoaded = false;
            }
        } finally {
            modelLock.writeLock().unlock();
        }
    }
}