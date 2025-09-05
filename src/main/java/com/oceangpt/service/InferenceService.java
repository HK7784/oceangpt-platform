package com.oceangpt.service;

import com.oceangpt.model.OceanData;
import com.oceangpt.model.PredictionResult;
import com.oceangpt.repository.PredictionResultRepository;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 推理服务类
 * 提供高级推理接口和业务逻辑处理
 */
@Service
public class InferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(InferenceService.class);
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
    private PredictionResultRepository predictionResultRepository;
    
    /**
     * 单点预测
     * @param oceanData 海洋数据
     * @param targetTimestamp 预测目标时间
     * @return 预测结果
     */
    @Cacheable(value = "predictions", key = "#oceanData.latitude + '_' + #oceanData.longitude + '_' + #targetTimestamp")
    public PredictionResult predictSinglePoint(OceanData oceanData, LocalDateTime targetTimestamp) {
        logger.info("执行单点预测 - 位置: ({}, {}), 目标时间: {}", 
                   oceanData.getLatitude(), oceanData.getLongitude(), targetTimestamp);
        
        try {
            // 创建输入张量
            INDArray inputTensor = createInputFromOceanData(oceanData, targetTimestamp);
            
            // 执行推理
            INDArray output = modelService.predict(inputTensor);
            
            // 解析结果
            ModelService.PredictionOutput predictionOutput = modelService.parsePredictionOutput(output);
            
            // 创建预测结果实体
            PredictionResult result = createPredictionResult(oceanData, targetTimestamp, predictionOutput);
            
            // 保存到数据库
            result = predictionResultRepository.save(result);
            
            logger.info("单点预测完成 - 结果: {}", predictionOutput);
            return result;
            
        } catch (Exception e) {
            logger.error("单点预测失败: {}", e.getMessage(), e);
            throw new RuntimeException("预测执行失败", e);
        }
    }
    
    /**
     * 批量预测
     * @param oceanDataList 海洋数据列表
     * @param targetTimestamp 预测目标时间
     * @return 预测结果列表
     */
    public List<PredictionResult> predictBatch(List<OceanData> oceanDataList, LocalDateTime targetTimestamp) {
        logger.info("执行批量预测 - 数据点数量: {}, 目标时间: {}", oceanDataList.size(), targetTimestamp);
        
        if (oceanDataList.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // 创建批量输入张量
            INDArray batchInput = createBatchInputFromOceanData(oceanDataList, targetTimestamp);
            
            // 执行批量推理
            INDArray batchOutput = modelService.batchPredict(batchInput);
            
            // 解析批量结果
            List<PredictionResult> results = new ArrayList<>();
            for (int i = 0; i < oceanDataList.size(); i++) {
                INDArray singleOutput = batchOutput.getRow(i);
                ModelService.PredictionOutput predictionOutput = modelService.parsePredictionOutput(singleOutput);
                
                PredictionResult result = createPredictionResult(oceanDataList.get(i), targetTimestamp, predictionOutput);
                results.add(result);
            }
            
            // 批量保存到数据库
            results = predictionResultRepository.saveAll(results);
            
            logger.info("批量预测完成 - 成功预测 {} 个数据点", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("批量预测失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量预测执行失败", e);
        }
    }
    
    /**
     * 异步预测
     * @param oceanData 海洋数据
     * @param targetTimestamp 预测目标时间
     * @return 异步预测结果
     */
    @Async
    public CompletableFuture<PredictionResult> predictAsync(OceanData oceanData, LocalDateTime targetTimestamp) {
        logger.info("启动异步预测任务 - 位置: ({}, {})", oceanData.getLatitude(), oceanData.getLongitude());
        
        try {
            PredictionResult result = predictSinglePoint(oceanData, targetTimestamp);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("异步预测失败: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 时间序列预测
     * @param oceanData 基础海洋数据
     * @param startTime 预测开始时间
     * @param endTime 预测结束时间
     * @param intervalHours 时间间隔（小时）
     * @return 时间序列预测结果
     */
    public List<PredictionResult> predictTimeSeries(OceanData oceanData, LocalDateTime startTime, 
                                                   LocalDateTime endTime, int intervalHours) {
        logger.info("执行时间序列预测 - 位置: ({}, {}), 时间范围: {} 到 {}, 间隔: {}小时",
                   oceanData.getLatitude(), oceanData.getLongitude(), startTime, endTime, intervalHours);
        
        List<PredictionResult> results = new ArrayList<>();
        LocalDateTime currentTime = startTime;
        
        while (!currentTime.isAfter(endTime)) {
            try {
                PredictionResult result = predictSinglePoint(oceanData, currentTime);
                results.add(result);
                currentTime = currentTime.plusHours(intervalHours);
            } catch (Exception e) {
                logger.warn("时间点 {} 预测失败: {}", currentTime, e.getMessage());
                currentTime = currentTime.plusHours(intervalHours);
            }
        }
        
        logger.info("时间序列预测完成 - 成功预测 {} 个时间点", results.size());
        return results;
    }
    
    /**
     * 区域预测
     * @param minLatitude 最小纬度
     * @param maxLatitude 最大纬度
     * @param minLongitude 最小经度
     * @param maxLongitude 最大经度
     * @param gridSize 网格大小（度）
     * @param targetTimestamp 预测目标时间
     * @param baseOceanData 基础海洋数据模板
     * @return 区域预测结果
     */
    public List<PredictionResult> predictRegion(double minLatitude, double maxLatitude,
                                               double minLongitude, double maxLongitude,
                                               double gridSize, LocalDateTime targetTimestamp,
                                               OceanData baseOceanData) {
        logger.info("执行区域预测 - 范围: ({}, {}) 到 ({}, {}), 网格大小: {}",
                   minLatitude, minLongitude, maxLatitude, maxLongitude, gridSize);
        
        List<OceanData> gridPoints = generateGridPoints(minLatitude, maxLatitude, 
                                                        minLongitude, maxLongitude, 
                                                        gridSize, baseOceanData);
        
        return predictBatch(gridPoints, targetTimestamp);
    }
    
    /**
     * 获取预测置信度
     * @param predictionResult 预测结果
     * @return 置信度分数 (0-1)
     */
    public double calculateConfidence(PredictionResult predictionResult) {
        // 基于多个因素计算置信度
        double confidence = 0.8; // 基础置信度
        
        // 根据数据完整性调整
        if (predictionResult.getPredictedTemperature() != null && 
            predictionResult.getPredictedSalinity() != null && 
            predictionResult.getPredictedPh() != null) {
            confidence += 0.1;
        }
        
        // 根据预测值合理性调整
        if (isPredictionReasonable(predictionResult)) {
            confidence += 0.1;
        } else {
            confidence -= 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * 从海洋数据创建输入张量
     */
    private INDArray createInputFromOceanData(OceanData oceanData, LocalDateTime targetTimestamp) {
        long timestamp = java.sql.Timestamp.valueOf(targetTimestamp).getTime();
        double latitude = oceanData.getLatitude();
        double longitude = oceanData.getLongitude();
        double temperature = oceanData.getSeaSurfaceTemperature() != null ? oceanData.getSeaSurfaceTemperature() : 15.0;
        double salinity = oceanData.getSalinity() != null ? oceanData.getSalinity() : 35.0;
        
        // 添加额外特征
        double[] additionalFeatures = {
            oceanData.getPhLevel() != null ? oceanData.getPhLevel() : 8.1,
            oceanData.getDissolvedOxygen() != null ? oceanData.getDissolvedOxygen() : 8.0,
            oceanData.getChlorophyllConcentration() != null ? oceanData.getChlorophyllConcentration() : 0.5
        };
        
        return modelService.createInputTensor(timestamp, latitude, longitude, temperature, salinity, additionalFeatures);
    }
    
    /**
     * 从海洋数据列表创建批量输入张量
     */
    private INDArray createBatchInputFromOceanData(List<OceanData> oceanDataList, LocalDateTime targetTimestamp) {
        List<INDArray> inputTensors = new ArrayList<>();
        
        for (OceanData oceanData : oceanDataList) {
            INDArray inputTensor = createInputFromOceanData(oceanData, targetTimestamp);
            inputTensors.add(inputTensor);
        }
        
        // 将所有输入张量堆叠成批量张量
        return org.nd4j.linalg.factory.Nd4j.vstack(inputTensors.toArray(new INDArray[0]));
    }
    
    /**
     * 创建预测结果实体
     */
    private PredictionResult createPredictionResult(OceanData oceanData, LocalDateTime targetTimestamp, 
                                                   ModelService.PredictionOutput predictionOutput) {
        PredictionResult result = new PredictionResult();
        result.setTargetTimestamp(targetTimestamp);
        result.setLatitude(oceanData.getLatitude());
        result.setLongitude(oceanData.getLongitude());
        result.setPredictedTemperature(oceanData.getSeaSurfaceTemperature());
        result.setPredictedSalinity(oceanData.getSalinity());
        result.setPredictedPh(predictionOutput.getPhLevel());
        
        // 根据污染指数设置污染等级
        String pollutionLevel = categorizePollutionLevel(predictionOutput.getPollutionIndex());
        result.setPredictedPollutionLevel(pollutionLevel);
        
        // 计算置信度
        double confidence = calculateConfidence(result);
        result.setConfidenceScore(confidence);
        
        // 设置模型版本
        result.setModelVersion("OceanGPT-14B-v1.0");
        
        // 添加元数据
        Map<String, String> metadata = new HashMap<>();
        metadata.put("nutrient_concentration", String.valueOf(predictionOutput.getNutrientConcentration()));
        metadata.put("dissolved_oxygen", String.valueOf(predictionOutput.getDissolvedOxygen()));
        metadata.put("pollution_index", String.valueOf(predictionOutput.getPollutionIndex()));
        result.setMetadata(metadata);
        
        return result;
    }
    
    /**
     * 生成网格点
     */
    private List<OceanData> generateGridPoints(double minLat, double maxLat, double minLon, double maxLon,
                                              double gridSize, OceanData template) {
        List<OceanData> gridPoints = new ArrayList<>();
        
        for (double lat = minLat; lat <= maxLat; lat += gridSize) {
            for (double lon = minLon; lon <= maxLon; lon += gridSize) {
                OceanData gridPoint = new OceanData();
                gridPoint.setLatitude(lat);
                gridPoint.setLongitude(lon);
                gridPoint.setTimestamp(template.getTimestamp());
                gridPoint.setSeaSurfaceTemperature(template.getSeaSurfaceTemperature());
                gridPoint.setSalinity(template.getSalinity());
                gridPoint.setPhLevel(template.getPhLevel());
                gridPoint.setDissolvedOxygen(template.getDissolvedOxygen());
                gridPoint.setChlorophyllConcentration(template.getChlorophyllConcentration());
                gridPoint.setDataSource("GRID_GENERATED");
                
                gridPoints.add(gridPoint);
            }
        }
        
        return gridPoints;
    }
    
    /**
     * 污染等级分类
     */
    private String categorizePollutionLevel(double pollutionIndex) {
        if (pollutionIndex < 20) {
            return "CLEAN";
        } else if (pollutionIndex < 40) {
            return "LIGHT";
        } else if (pollutionIndex < 60) {
            return "MODERATE";
        } else if (pollutionIndex < 80) {
            return "HEAVY";
        } else {
            return "SEVERE";
        }
    }
    
    /**
     * 检查预测值是否合理
     */
    private boolean isPredictionReasonable(PredictionResult result) {
        // 检查pH值范围
        if (result.getPredictedPh() != null && 
            (result.getPredictedPh() < 7.0 || result.getPredictedPh() > 9.0)) {
            return false;
        }
        
        // 检查温度范围
        if (result.getPredictedTemperature() != null && 
            (result.getPredictedTemperature() < -2.0 || result.getPredictedTemperature() > 40.0)) {
            return false;
        }
        
        // 检查盐度范围
        if (result.getPredictedSalinity() != null && 
            (result.getPredictedSalinity() < 25.0 || result.getPredictedSalinity() > 45.0)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取模型状态
     */
    public Map<String, Object> getModelStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("modelReady", modelService.isModelReady());
        status.put("modelInfo", modelService.getModelInfo());
        status.put("lastPredictionTime", LocalDateTime.now());
        
        return status;
    }
}