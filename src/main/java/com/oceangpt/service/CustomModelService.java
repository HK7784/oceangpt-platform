package com.oceangpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 自定义模型服务
 * 负责调用用户的EndToEndRegressionModel进行水质预测
 */
@Service
public class CustomModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomModelService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private WaterQualityClassificationService waterQualityClassificationService;

    @Value("${oceangpt.python.executable:python}")
    private String pythonExecutable;
    
    @Value("${oceangpt.model.path:}")
    private String modelPath;
    
    @Value("${oceangpt.model.timeout:30}")
    private int timeoutSeconds;

    @Value("${oceangpt.model.script-path:/app/model_predictor.py}")
    private String pythonScriptPath;
    
    public PredictionResponse predictWaterQuality(PredictionRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("开始使用自定义模型进行预测: 经度={}, 纬度={}", request.getLongitude(), request.getLatitude());
            Map<String, Object> inputData = prepareInputData(request);
            Map<String, Object> result = callPythonModel(inputData);
            PredictionResponse response = buildResponse(result, request);
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs(processingTime);
            logger.info("预测完成，耗时: {}ms", processingTime);
            return response;
        } catch (Exception e) {
            logger.error("预测失败", e);
            return createErrorResponse(e.getMessage(), request);
        }
    }

    public Map<String, PredictionResponse> predictMultipleTargets(PredictionRequest request) {
        PredictionResponse response = predictWaterQuality(request);
        Map<String, PredictionResponse> predictions = new HashMap<>();
        
        PredictionResponse dinResp = copyResponse(response);
        if (response.isSuccess() && response.getDinLevel() != null) {
            dinResp.setSuccess(true);
            dinResp.setWaterQualityLevel(determineWaterQualityLevel(response.getDinLevel(), "DIN"));
        }
        predictions.put("DIN", dinResp);
        
        PredictionResponse srpResp = copyResponse(response);
        if (response.isSuccess() && response.getSrpLevel() != null) {
            srpResp.setSuccess(true);
            srpResp.setWaterQualityLevel(determineWaterQualityLevel(response.getSrpLevel(), "SRP"));
        }
        predictions.put("SRP", srpResp);
        
        PredictionResponse phResp = copyResponse(response);
        if (response.isSuccess() && response.getPhLevel() != null) {
            phResp.setSuccess(true);
            phResp.setWaterQualityLevel(determineWaterQualityLevel(response.getPhLevel(), "pH"));
        }
        predictions.put("pH", phResp);
        
        return predictions;
    }
    
    // --- 核心修复：Python模型调用逻辑 ---
    private Map<String, Object> callPythonModel(Map<String, Object> inputData) throws Exception {
        Path scriptPath = null;
        
        // 1. 优先检查 Docker 容器内的标准路径 (修复点)
        Path dockerDefaultPath = Paths.get("/app/model_predictor.py");
        if (Files.exists(dockerDefaultPath)) {
            scriptPath = dockerDefaultPath;
            logger.info("Found script at Docker default path: {}", scriptPath);
        }
        
        // 2. 尝试配置文件路径
        if (scriptPath == null && pythonScriptPath != null && !pythonScriptPath.trim().isEmpty()) {
            Path p = Paths.get(pythonScriptPath.trim());
            if (Files.exists(p)) {
                scriptPath = p;
                logger.info("Using oceangpt.model.script-path: {}", p);
            }
        }
        
        // 3. 尝试从Classpath提取 (本地开发兜底)
        if (scriptPath == null) {
            logger.info("Extracting script from Classpath...");
            scriptPath = extractScriptToTemp("model_predictor.py");
        }
        
        if (scriptPath == null || !Files.exists(scriptPath)) {
            throw new RuntimeException("Python script not found: model_predictor.py");
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        String resolvedPython = resolvePythonExecutable();
        processBuilder.environment().put("KMP_DUPLICATE_LIB_OK", "TRUE");
        
        if (modelPath != null && !modelPath.trim().isEmpty()) {
            processBuilder.command(resolvedPython, scriptPath.toString(), "--model-path", modelPath);
        } else {
            processBuilder.command(resolvedPython, scriptPath.toString());
        }
        
        processBuilder.directory(Paths.get(".").toAbsolutePath().toFile());
        Process process = processBuilder.start();
        
        try (OutputStream os = process.getOutputStream()) {
            objectMapper.writeValue(os, inputData);
            os.flush();
        }
        
        // 读取输出... (标准处理逻辑)
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            while ((line = errorReader.readLine()) != null) errorOutput.append(line).append("\n");
        }
        
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python模型调用超时");
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Python模型调用失败: " + errorOutput.toString().trim());
        }
        
        return objectMapper.convertValue(objectMapper.readTree(output.toString().trim()), Map.class);
    }

    // 辅助方法...
    private PredictionResponse copyResponse(PredictionResponse original) {
        PredictionResponse copy = new PredictionResponse();
        copy.setSuccess(original.isSuccess());
        copy.setErrorMessage(original.getErrorMessage());
        copy.setLatitude(original.getLatitude());
        copy.setLongitude(original.getLongitude());
        // ... 复制其他字段
        copy.setDinLevel(original.getDinLevel());
        copy.setSrpLevel(original.getSrpLevel());
        copy.setPhLevel(original.getPhLevel());
        return copy;
    }
    
    private String determineWaterQualityLevel(Double value, String type) {
        return "II类"; // 简化示例，实际应根据阈值判断
    }
    
    private PredictionResponse createErrorResponse(String error, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setErrorMessage(error);
        return response;
    }
    
    private Path extractScriptToTemp(String resourceName) {
        try {
            ClassPathResource resource = new ClassPathResource("python/" + resourceName);
            if (!resource.exists()) resource = new ClassPathResource(resourceName);
            if (!resource.exists()) return null;
            Path tempScript = Files.createTempFile("oceangpt_model_", ".py");
            Files.copy(resource.getInputStream(), tempScript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempScript;
        } catch (IOException e) {
            return null;
        }
    }
    
    private String resolvePythonExecutable() {
        return pythonExecutable; // 简化，实际代码包含完整的回退逻辑
    }
    
    private Map<String, Object> prepareInputData(PredictionRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", request.getLatitude());
        data.put("longitude", request.getLongitude());
        // ... 添加其他必要的输入字段
        return data;
    }
}
