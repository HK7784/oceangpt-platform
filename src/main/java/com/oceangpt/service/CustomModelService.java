package com.oceangpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import com.oceangpt.dto.SatelliteDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
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

    @Autowired
    private DataProcessingService dataProcessingService;
    
    @Autowired
    private DataInterpolationService dataInterpolationService;
    
    @Value("${oceangpt.python.executable:python}")
    private String pythonExecutable;
    
    @Value("${oceangpt.model.path:}")
    private String modelPath;
    
    @Value("${oceangpt.model.timeout:30}")
    private int timeoutSeconds;

    @Value("${oceangpt.model.script-path:/app/model_predictor.py}")
    private String pythonScriptPath;
    
    /**
     * 使用自定义模型进行水质预测
     * 
     * @param request 预测请求
     * @return 预测响应
     */
    public PredictionResponse predictWaterQuality(PredictionRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("开始使用自定义模型进行预测: 经度={}, 纬度={}", 
                       request.getLongitude(), request.getLatitude());
            
            // 准备输入数据
            Map<String, Object> inputData = prepareInputData(request);
    
            // 调用Python模型
            Map<String, Object> result = callPythonModel(inputData);
            
            // 构建响应
            PredictionResponse response = buildResponse(result, request);
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs((int) processingTime);
            
            logger.info("预测完成，耗时: {}ms", processingTime);
            return response;
            
        } catch (Exception e) {
            logger.error("预测失败", e);
            return createErrorResponse(e.getMessage(), request);
        }
    }
    
    /**
     * 准备Python模型的输入数据
     */
    private Map<String, Object> prepareInputData(PredictionRequest request) {
        Map<String, Object> inputData = new HashMap<>();
        
        // 基本信息
        inputData.put("latitude", request.getLatitude());
        inputData.put("longitude", request.getLongitude());
        // 时间信息（用于按月预测）
        if (request.getDateTime() != null) {
            inputData.put("month", request.getDateTime().getMonthValue());
            inputData.put("year", request.getDateTime().getYear());
        }
        
        // S2光谱数据 (13个波段) - 组装成数组
        java.util.List<Double> s2Data = new java.util.ArrayList<>();
        s2Data.add(request.getS2B2() != null ? request.getS2B2() : 0.0);
        s2Data.add(request.getS2B3() != null ? request.getS2B3() : 0.0);
        s2Data.add(request.getS2B4() != null ? request.getS2B4() : 0.0);
        s2Data.add(request.getS2B5() != null ? request.getS2B5() : 0.0);
        s2Data.add(request.getS2B6() != null ? request.getS2B6() : 0.0);
        s2Data.add(request.getS2B7() != null ? request.getS2B7() : 0.0);
        s2Data.add(request.getS2B8() != null ? request.getS2B8() : 0.0);
        s2Data.add(request.getS2B8A() != null ? request.getS2B8A() : 0.0);
        // 补充到13个波段
        while (s2Data.size() < 13) {
            s2Data.add(0.0);
        }
        inputData.put("s2Data", s2Data);
        
        // S3光谱数据 (21个波段) - 组装成数组
        java.util.List<Double> s3Data = new java.util.ArrayList<>();
        s3Data.add(request.getS3Oa01() != null ? request.getS3Oa01() : 0.0);
        s3Data.add(request.getS3Oa02() != null ? request.getS3Oa02() : 0.0);
        s3Data.add(request.getS3Oa03() != null ? request.getS3Oa03() : 0.0);
        s3Data.add(request.getS3Oa04() != null ? request.getS3Oa04() : 0.0);
        s3Data.add(request.getS3Oa05() != null ? request.getS3Oa05() : 0.0);
        s3Data.add(request.getS3Oa06() != null ? request.getS3Oa06() : 0.0);
        s3Data.add(request.getS3Oa07() != null ? request.getS3Oa07() : 0.0);
        s3Data.add(request.getS3Oa08() != null ? request.getS3Oa08() : 0.0);
        // 补充到21个波段
        while (s3Data.size() < 21) {
            s3Data.add(0.0);
        }
        inputData.put("s3Data", s3Data);
        
        // 神经网络预测值（可选字段）
        inputData.put("chlNN", request.getChlNN() != null ? request.getChlNN() : 0.0);
        inputData.put("tsmNN", request.getTsmNN() != null ? request.getTsmNN() : 0.0);
        
        return inputData;
    }
    
    /**
     * 调用Python模型进行预测
     */
    private Map<String, Object> callPythonModel(Map<String, Object> inputData) throws Exception {
        Path scriptPath = null;
        
        // 1. 尝试使用环境变量配置的路径
        String envScript = System.getenv("PYTHON_SCRIPT_PATH");
        if (envScript != null && !envScript.trim().isEmpty()) {
            Path p = Paths.get(envScript.trim());
            if (Files.exists(p)) {
                scriptPath = p;
                logger.info("使用环境变量PYTHON_SCRIPT_PATH指定的脚本: {}", p);
            } else {
                logger.warn("环境变量PYTHON_SCRIPT_PATH指定的文件不存在: {}", p);
            }
        }

        // 2. 尝试Docker容器默认路径 /app/model_predictor.py
        if (scriptPath == null) {
            Path p = Paths.get("/app/model_predictor.py");
            if (Files.exists(p)) {
                scriptPath = p;
                logger.info("找到容器默认路径脚本: {}", p);
            }
        }
        
        // 3. 尝试配置文件路径
        if (scriptPath == null && pythonScriptPath != null && !pythonScriptPath.trim().isEmpty()) {
            Path p = Paths.get(pythonScriptPath.trim());
            if (Files.exists(p)) {
                scriptPath = p;
                logger.info("使用配置项oceangpt.model.script-path指定的脚本: {}", p);
            }
        }
        
        // 4. 尝试从Classpath提取
        if (scriptPath == null) {
            logger.info("尝试从Classpath提取脚本...");
            scriptPath = extractScriptToTemp("model_predictor.py");
            if (scriptPath != null) {
                logger.info("脚本已提取到临时文件: {}", scriptPath);
            }
        }
        
        if (scriptPath == null || !Files.exists(scriptPath)) {
            throw new RuntimeException("未找到Python脚本: model_predictor.py (已尝试Env, /app/, Config, Classpath)");
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String resolvedPython = resolvePythonExecutable();
            java.util.Map<String, String> env = processBuilder.environment();
            env.put("KMP_DUPLICATE_LIB_OK", "TRUE");
            if (modelPath != null && !modelPath.trim().isEmpty()) {
                processBuilder.command(resolvedPython, scriptPath.toString(), "--model-path", modelPath);
            } else {
                processBuilder.command(resolvedPython, scriptPath.toString());
            }
            processBuilder.directory(Paths.get(".").toAbsolutePath().toFile());
            logger.debug("执行命令: {}", String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            try (OutputStream os = process.getOutputStream()) {
                objectMapper.writeValue(os, inputData);
                os.flush();
            }
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python模型调用超时");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorMsg = errorOutput.toString().trim();
                if (errorMsg.isEmpty()) {
                    errorMsg = "无错误输出 (可能因内存不足被系统终止，exitCode=" + exitCode + ")";
                }
                logger.error("Python模型调用失败，退出码: {}, 错误输出: {}", exitCode, errorMsg);
                throw new RuntimeException("Python模型调用失败: " + errorMsg);
            }
            String outputStr = output.toString().trim();
            logger.debug("Python模型输出: {}", outputStr);
            try {
                JsonNode jsonNode = objectMapper.readTree(outputStr);
                return objectMapper.convertValue(jsonNode, Map.class);
            } catch (Exception e) {
                logger.error("解析Python模型输出失败: {}", outputStr, e);
                throw new RuntimeException("解析模型输出失败: " + e.getMessage());
            }
        } finally {
        }
    }

    /**
     * 解析与回退 Python 可执行路径，兼容容器与本地环境
     */
    private String resolvePythonExecutable() {
        // 优先使用配置与环境变量
        String candidate = this.pythonExecutable != null ? this.pythonExecutable.trim() : "";
        if (canRun(candidate)) {
            return candidate;
        }

        String envPython = System.getenv("PYTHON_EXECUTABLE");
        if (canRun(envPython)) {
            return envPython;
        }

        // 平台候选列表
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> fallbacks;
        if (os.contains("win")) {
            fallbacks = Arrays.asList(
                "python",
                "python3",
                "py"
            );
        } else {
            fallbacks = Arrays.asList(
                "/app/venv/bin/python3",
                "/usr/local/bin/python3",
                "/usr/bin/python3",
                "python3",
                "python"
            );
        }

        for (String fb : fallbacks) {
            if (canRun(fb)) {
                return fb;
            }
        }

        // 最后兜底
        return "python";
    }
    
    private boolean canRun(String cmd) {
        if (cmd == null || cmd.isEmpty()) return false;
        try {
            Process p = new ProcessBuilder(cmd, "--version").start();
            boolean finished = p.waitFor(2, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Path extractScriptToTemp(String resourceName) throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(resourceName);
            if (!resource.exists()) return null;
            Path tempFile = Files.createTempFile("oceangpt-", "-" + resourceName);
            Files.copy(resource.getInputStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            logger.warn("提取脚本失败: {}", e.getMessage());
            return null;
        }
    }

    // 辅助方法：构建响应对象 (代码省略，未变更)
    private PredictionResponse buildResponse(Map<String, Object> result, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess((Boolean) result.getOrDefault("success", false));
        response.setPredictionId(java.util.UUID.randomUUID().toString());
        response.setTimestamp(System.currentTimeMillis());
        
        if (result.containsKey("predictions")) {
            Map<String, Object> preds = (Map<String, Object>) result.get("predictions");
            response.setDinLevel(getDouble(preds, "DIN"));
            response.setSrpLevel(getDouble(preds, "SRP"));
            response.setPhLevel(getDouble(preds, "pH"));
        }
        
        response.setWaterQualityLevel((String) result.getOrDefault("qualityLevel", "UNKNOWN"));
        response.setConfidence(getDouble(result, "confidence"));
        response.setModelVersion((String) result.getOrDefault("modelVersion", "Unknown"));
        
        return response;
    }
    
    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
    
    private PredictionResponse createErrorResponse(String error, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setMessage(error);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // 多目标预测 (用于报告生成)
    public Map<String, PredictionResponse> predictMultipleTargets(PredictionRequest request) {
        PredictionResponse mainResp = predictWaterQuality(request);
        Map<String, PredictionResponse> map = new HashMap<>();
        
        // 构造子响应
        PredictionResponse din = new PredictionResponse();
        din.setSuccess(mainResp.isSuccess());
        din.setDinLevel(mainResp.getDinLevel());
        din.setConfidence(mainResp.getConfidence());
        map.put("DIN", din);
        
        PredictionResponse srp = new PredictionResponse();
        srp.setSuccess(mainResp.isSuccess());
        srp.setSrpLevel(mainResp.getSrpLevel());
        srp.setConfidence(mainResp.getConfidence());
        map.put("SRP", srp);
        
        PredictionResponse ph = new PredictionResponse();
        ph.setSuccess(mainResp.isSuccess());
        ph.setPhLevel(mainResp.getPhLevel());
        ph.setConfidence(mainResp.getConfidence());
        map.put("pH", ph);
        
        return map;
    }
}
