package com.oceangpt.service;

import com.oceangpt.config.ModelConfig;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * OceanGPT模型服务类
 * 负责模型加载、初始化和推理功能
 */
@Service
public class ModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);
    
    @Autowired
    private ModelConfig modelConfig;
    
    @Autowired
    private DjlModelService djlModelService;
    
    private ComputationGraph model;
    private boolean modelLoaded = false;
    private final ReentrantReadWriteLock modelLock = new ReentrantReadWriteLock();
    
    /**
     * 模型初始化
     * 在Spring容器启动后自动执行
     */
    @PostConstruct
    public void initializeModel() {
        try {
            loadModel();
        } catch (Exception e) {
            logger.error("模型初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("OceanGPT模型初始化失败", e);
        }
    }
    
    /**
     * 手动初始化模型
     * @return 初始化是否成功
     */
    public boolean tryInitializeModel() {
        try {
            loadModel();
            return true;
        } catch (Exception e) {
            logger.error("模型初始化失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 加载OceanGPT模型
     */
    private void loadModel() throws IOException {
        modelLock.writeLock().lock();
        try {
            // 检查是否为mock模式
            if (modelConfig.isMockMode()) {
                logger.info("运行在Mock模式下，跳过实际模型加载");
                modelLoaded = true;
                return;
            }
            
            logger.info("开始加载OceanGPT模型: {}", modelConfig.getPath());
            
            File modelFile = new File(modelConfig.getPath());
            if (!modelFile.exists()) {
                throw new IOException("模型文件不存在: " + modelConfig.getPath());
            }
            
            // 检查文件大小
            long fileSizeGB = modelFile.length() / (1024 * 1024 * 1024);
            logger.info("模型文件大小: {} GB", fileSizeGB);
            
            if (fileSizeGB > 10) {
                logger.warn("模型文件较大 ({} GB)，加载可能需要较长时间", fileSizeGB);
            }
            
            // 检查文件类型并选择合适的加载方式
            String fileName = modelFile.getName().toLowerCase();
            long startTime = System.currentTimeMillis();
            
            if (fileName.endsWith(".pth")) {
                // PyTorch模型，使用DJL加载
                logger.info("检测到PyTorch模型文件，使用DJL加载");
                // 委托给DjlModelService处理
                if (djlModelService.isModelReady()) {
                    logger.info("DJL模型加载成功");
                    modelLoaded = true;
                } else {
                    throw new RuntimeException("DJL模型加载失败");
                }
            } else {
                // DL4J模型格式
                logger.info("使用DL4J加载模型");
                model = ModelSerializer.restoreComputationGraph(modelFile);
                logger.info("模型输入层数量: {}", model.getNumInputArrays());
                logger.info("模型输出层数量: {}", model.getNumOutputArrays());
                modelLoaded = true;
            }
            
            long loadTime = System.currentTimeMillis() - startTime;
            logger.info("模型加载成功，耗时: {} ms", loadTime);
            
        } finally {
            modelLock.writeLock().unlock();
        }
    }
    
    /**
     * 执行模型推理
     * @param inputData 输入数据（时间戳、经纬度、海洋参数等）
     * @return 预测结果（营养盐浓度、pH值等）
     */
    public INDArray predict(INDArray inputData) {
        if (!isModelReady()) {
            throw new IllegalStateException("模型未就绪，无法执行推理");
        }
        
        long startTime = System.currentTimeMillis();
        modelLock.readLock().lock();
        try {
            logger.debug("执行模型推理，输入形状: {}", inputData.shapeInfoToString());
            
            // 检查是否使用DJL模型
            String modelPath = modelConfig.getPath().toLowerCase();
            if (modelPath.endsWith(".pth")) {
                // 使用DJL进行推理
                float[] inputArray = inputData.toFloatVector();
                try {
                    float[] output = djlModelService.predict(inputArray);
                    return Nd4j.create(output).reshape(1, output.length);
                } catch (Exception e) {
                    throw new RuntimeException("DJL推理失败: " + e.getMessage(), e);
                }
            } else {
                // 使用DL4J进行推理
                INDArray[] outputs = model.output(inputData);
                return outputs[0];
            }
            
        } finally {
            long inferenceTime = System.currentTimeMillis() - startTime;
            logger.debug("推理完成，耗时: {} ms", inferenceTime);
            modelLock.readLock().unlock();
        }
    }
    
    /**
     * 批量推理
     * @param batchInputData 批量输入数据
     * @return 批量预测结果
     */
    public INDArray batchPredict(INDArray batchInputData) {
        if (!isModelReady()) {
            throw new IllegalStateException("模型未就绪，无法执行批量推理");
        }
        
        modelLock.readLock().lock();
        try {
            logger.debug("执行批量推理，批次大小: {}", batchInputData.size(0));
            
            long startTime = System.currentTimeMillis();
            INDArray[] outputs = model.output(batchInputData);
            long inferenceTime = System.currentTimeMillis() - startTime;
            
            logger.debug("批量推理完成，耗时: {} ms，平均每样本: {} ms", 
                        inferenceTime, inferenceTime / batchInputData.size(0));
            
            return outputs[0];
            
        } finally {
            modelLock.readLock().unlock();
        }
    }
    
    /**
     * 创建输入数据张量
     * @param timestamp 时间戳（Unix时间戳）
     * @param latitude 纬度
     * @param longitude 经度
     * @param seaTemperature 海表温度
     * @param salinity 盐度
     * @param additionalFeatures 其他特征（可选）
     * @return 格式化的输入张量
     */
    public INDArray createInputTensor(long timestamp, double latitude, double longitude, 
                                     double seaTemperature, double salinity, 
                                     double... additionalFeatures) {
        
        // 计算总特征数量
        int totalFeatures = 5 + additionalFeatures.length; // 时间戳、纬度、经度、温度、盐度 + 额外特征
        
        // 创建输入数组
        double[] inputArray = new double[totalFeatures];
        inputArray[0] = normalizeTimestamp(timestamp);
        inputArray[1] = normalizeLatitude(latitude);
        inputArray[2] = normalizeLongitude(longitude);
        inputArray[3] = normalizeTemperature(seaTemperature);
        inputArray[4] = normalizeSalinity(salinity);
        
        // 添加额外特征
        System.arraycopy(additionalFeatures, 0, inputArray, 5, additionalFeatures.length);
        
        // 创建形状为 [1, features] 的张量
        return Nd4j.create(inputArray).reshape(1, totalFeatures);
    }
    
    /**
     * 解析预测结果
     * @param output 模型输出张量
     * @return 解析后的预测结果
     */
    public PredictionOutput parsePredictionOutput(INDArray output) {
        // 假设模型输出格式：[营养盐浓度, pH值, 溶解氧, 污染指数]
        double[] outputArray = output.toDoubleVector();
        
        PredictionOutput result = new PredictionOutput();
        if (outputArray.length >= 4) {
            result.setNutrientConcentration(denormalizeNutrient(outputArray[0]));
            result.setPhLevel(denormalizePh(outputArray[1]));
            result.setDissolvedOxygen(denormalizeOxygen(outputArray[2]));
            result.setPollutionIndex(denormalizePollution(outputArray[3]));
        }
        
        return result;
    }
    
    /**
     * 检查模型是否就绪
     */
    public boolean isModelReady() {
        if (modelConfig.isMockMode()) {
            return modelLoaded;
        }
        
        // 检查模型类型
        String modelPath = modelConfig.getPath().toLowerCase();
        if (modelPath.endsWith(".pth")) {
            // PyTorch模型，检查DJL服务状态
            return modelLoaded && djlModelService.isModelReady();
        } else {
            // DL4J模型
            return modelLoaded && model != null;
        }
    }
    
    /**
     * 获取模型信息
     */
    public Map<String, Object> getModelInfo() {
        Map<String, Object> modelInfo = new HashMap<>();
        
        if (!isModelReady()) {
            modelInfo.put("status", "模型未加载");
            modelInfo.put("modelPath", modelConfig.getModelPath());
            modelInfo.put("batchSize", modelConfig.getBatchSize());
            modelInfo.put("maxSequenceLength", modelConfig.getMaxSequenceLength());
            modelInfo.put("inferenceThreads", modelConfig.getInferenceThreads());
            modelInfo.put("mockMode", modelConfig.isMockMode());
            return modelInfo;
        }
        
        modelInfo.put("status", modelConfig.isMockMode() ? "Mock模式已加载" : "已加载");
        modelInfo.put("modelPath", modelConfig.getModelPath());
        modelInfo.put("batchSize", modelConfig.getBatchSize());
        modelInfo.put("maxSequenceLength", modelConfig.getMaxSequenceLength());
        modelInfo.put("inferenceThreads", modelConfig.getInferenceThreads());
        modelInfo.put("mockMode", modelConfig.isMockMode());
        
        if (modelConfig.isMockMode()) {
            // Mock模式下提供模拟的模型信息
            modelInfo.put("inputLayers", 1);
            modelInfo.put("outputLayers", 1);
            modelInfo.put("parameters", 1000000);
        } else {
            modelInfo.put("inputLayers", model.getNumInputArrays());
            modelInfo.put("outputLayers", model.getNumOutputArrays());
            modelInfo.put("parameters", model.numParams());
        }
        
        return modelInfo;
    }
    
    /**
     * 数据标准化方法
     */
    private double normalizeTimestamp(long timestamp) {
        // 将时间戳标准化到 [0, 1] 范围
        // 假设基准时间为2020年1月1日
        long baseTimestamp = 1577836800000L; // 2020-01-01 00:00:00 UTC
        long maxTimestamp = 1893456000000L;  // 2030-01-01 00:00:00 UTC
        return (double) (timestamp - baseTimestamp) / (maxTimestamp - baseTimestamp);
    }
    
    private double normalizeLatitude(double latitude) {
        // 纬度范围 [-90, 90] 标准化到 [-1, 1]
        return latitude / 90.0;
    }
    
    private double normalizeLongitude(double longitude) {
        // 经度范围 [-180, 180] 标准化到 [-1, 1]
        return longitude / 180.0;
    }
    
    private double normalizeTemperature(double temperature) {
        // 海表温度范围 [-2, 35] 标准化到 [0, 1]
        return (temperature + 2.0) / 37.0;
    }
    
    private double normalizeSalinity(double salinity) {
        // 盐度范围 [30, 40] 标准化到 [0, 1]
        return (salinity - 30.0) / 10.0;
    }
    
    /**
     * 数据反标准化方法
     */
    private double denormalizeNutrient(double normalized) {
        // 营养盐浓度范围 [0, 50] μmol/L
        return normalized * 50.0;
    }
    
    private double denormalizePh(double normalized) {
        // pH值范围 [7.5, 8.5]
        return normalized * 1.0 + 7.5;
    }
    
    private double denormalizeOxygen(double normalized) {
        // 溶解氧范围 [0, 15] mg/L
        return normalized * 15.0;
    }
    
    private double denormalizePollution(double normalized) {
        // 污染指数范围 [0, 100]
        return normalized * 100.0;
    }
    
    /**
     * 标准化输入数组
     * @param input 原始输入数组
     * @return 标准化后的输入数组
     */
    public double[] normalizeInput(double[] input) {
        double[] normalized = new double[input.length];
        
        // 根据输入数组的长度和内容进行标准化
        for (int i = 0; i < input.length; i++) {
            if (i == 0) {
                // 假设第一个是温度
                normalized[i] = normalizeTemperature(input[i]);
            } else if (i == 1) {
                // 假设第二个是盐度
                normalized[i] = normalizeSalinity(input[i]);
            } else {
                // 其他参数简单标准化到[-1, 1]范围
                normalized[i] = Math.max(-1.0, Math.min(1.0, input[i] / 10.0));
            }
        }
        
        return normalized;
    }
    
    /**
     * 反标准化输出数组
     * @param normalizedOutput 标准化的输出数组
     * @return 反标准化后的输出数组
     */
    public double[] denormalizeOutput(double[] normalizedOutput) {
        double[] denormalized = new double[normalizedOutput.length];
        
        if (normalizedOutput.length >= 4) {
            denormalized[0] = denormalizeNutrient(normalizedOutput[0]);  // 营养盐浓度
            denormalized[1] = denormalizePh(normalizedOutput[1]);        // pH值
            denormalized[2] = denormalizeOxygen(normalizedOutput[2]);    // 溶解氧
            denormalized[3] = denormalizePollution(normalizedOutput[3]); // 污染指数
        }
        
        // 如果有更多元素，直接复制
        for (int i = 4; i < normalizedOutput.length; i++) {
            denormalized[i] = normalizedOutput[i];
        }
        
        return denormalized;
    }
    
    /**
     * 资源清理
     */
    @PreDestroy
    public void cleanup() {
        modelLock.writeLock().lock();
        try {
            if (model != null) {
                logger.info("清理模型资源");
                // DL4J模型会自动管理内存，这里主要是标记清理
                model = null;
                modelLoaded = false;
            }
        } finally {
            modelLock.writeLock().unlock();
        }
    }
    
    /**
     * 预测输出结果类
     */
    public static class PredictionOutput {
        private double nutrientConcentration; // 营养盐浓度
        private double phLevel;              // pH值
        private double dissolvedOxygen;      // 溶解氧
        private double pollutionIndex;       // 污染指数
        
        // Getters and Setters
        public double getNutrientConcentration() {
            return nutrientConcentration;
        }
        
        public void setNutrientConcentration(double nutrientConcentration) {
            this.nutrientConcentration = nutrientConcentration;
        }
        
        public double getPhLevel() {
            return phLevel;
        }
        
        public void setPhLevel(double phLevel) {
            this.phLevel = phLevel;
        }
        
        public double getDissolvedOxygen() {
            return dissolvedOxygen;
        }
        
        public void setDissolvedOxygen(double dissolvedOxygen) {
            this.dissolvedOxygen = dissolvedOxygen;
        }
        
        public double getPollutionIndex() {
            return pollutionIndex;
        }
        
        public void setPollutionIndex(double pollutionIndex) {
            this.pollutionIndex = pollutionIndex;
        }
        
        @Override
        public String toString() {
            return String.format("PredictionOutput{营养盐=%.2f μmol/L, pH=%.2f, 溶解氧=%.2f mg/L, 污染指数=%.2f}",
                               nutrientConcentration, phLevel, dissolvedOxygen, pollutionIndex);
        }
    }
}