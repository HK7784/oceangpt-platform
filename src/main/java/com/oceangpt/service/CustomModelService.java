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
     * 多目标预测（适配ReportGenerationService）
     * 将单次预测结果映射为多目标Map格式
     */
    public Map<String, PredictionResponse> predictMultipleTargets(PredictionRequest request) {
        // 调用主预测方法
        PredictionResponse response = predictWaterQuality(request);
        
        Map<String, PredictionResponse> predictions = new HashMap<>();
        
        // 这里的逻辑是：一次预测包含了所有指标（DIN, SRP, pH）
        // 我们将同一个响应对象（或其副本）映射到不同的键
        // 这样 ReportGenerationService 可以通过 get("DIN") 获取包含 DIN 数据的响应
        
        // 1. DIN 预测
        PredictionResponse dinResp = copyResponse(response);
        // 如果主响应成功且包含DIN，则该子响应有效
        if (response.isSuccess() && response.getDinLevel() != null) {
            dinResp.setSuccess(true);
            dinResp.setWaterQualityLevel(determineWaterQualityLevel(response.getDinLevel(), "DIN"));
        }
        predictions.put("DIN", dinResp);
        
        // 2. SRP 预测
        PredictionResponse srpResp = copyResponse(response);
        if (response.isSuccess() && response.getSrpLevel() != null) {
            srpResp.setSuccess(true);
            srpResp.setWaterQualityLevel(determineWaterQualityLevel(response.getSrpLevel(), "SRP"));
        }
        predictions.put("SRP", srpResp);
        
        // 3. pH 预测
        PredictionResponse phResp = copyResponse(response);
        if (response.isSuccess() && response.getPhLevel() != null) {
            phResp.setSuccess(true);
            phResp.setWaterQualityLevel(determineWaterQualityLevel(response.getPhLevel(), "pH"));
        }
        predictions.put("pH", phResp);
        
        return predictions;
    }
    
    private PredictionResponse copyResponse(PredictionResponse original) {
        PredictionResponse copy = new PredictionResponse();
        copy.setSuccess(original.isSuccess());
        copy.setErrorMessage(original.getErrorMessage());
        copy.setLatitude(original.getLatitude());
        copy.setLongitude(original.getLongitude());
        copy.setPredictionTimestamp(original.getPredictionTimestamp());
        copy.setProcessingTimeMs(original.getProcessingTimeMs());
        copy.setConfidence(original.getConfidence());
        copy.setModelVersion(original.getModelVersion());
        copy.setDinLevel(original.getDinLevel());
        copy.setDinUnit(original.getDinUnit());
        copy.setSrpLevel(original.getSrpLevel());
        copy.setSrpUnit(original.getSrpUnit());
        copy.setPhLevel(original.getPhLevel());
        copy.setPhUnit(original.getPhUnit());
        copy.setChlLevel(original.getChlLevel());
        copy.setChlUnit(original.getChlUnit());
        copy.setWaterQualityLevel(original.getWaterQualityLevel());
        copy.setQualityLevel(original.getQualityLevel());
        copy.setAdditionalInfo(original.getAdditionalInfo());
        return copy;
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
            // 可以在这里清理临时文件，但为了性能通常保留
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

    /**
     * 简单检测可执行是否可调用（运行 --version 即可）
     */
    private boolean canRun(String exe) {
        if (exe == null || exe.isEmpty()) return false;
        try {
            Process p = new ProcessBuilder(exe, "--version").start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            int code = p.exitValue();
            return code == 0 || code == 1; // 部分发行版返回1也表示打印了版本
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从classpath提取Python脚本到临时文件，返回可执行路径
     */
    private Path extractScriptToTemp(String resourceName) {
        try {
            ClassPathResource resource = new ClassPathResource("python/" + resourceName);
            if (!resource.exists()) {
                resource = new ClassPathResource(resourceName);
                if (!resource.exists()) {
                    logger.error("Classpath中未找到脚本: python/{} 或 {}", resourceName, resourceName);
                    return null;
                }
            }
            Path tempScript = Files.createTempFile("oceangpt_model_predictor_", ".py");
            Files.copy(resource.getInputStream(), tempScript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempScript;
        } catch (IOException e) {
            logger.error("提取Python脚本失败", e);
            return null;
        }
    }
    
    /**
     * 构建预测响应
     */
    private PredictionResponse buildResponse(Map<String, Object> result, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        
        // 检查是否成功
        Boolean success = (Boolean) result.get("success");
        if (success == null || !success) {
            response.setSuccess(false);
            response.setErrorMessage(result.get("error") != null ? result.get("error").toString() : "预测失败");
            return response;
        }
        
        // 设置预测结果
        response.setSuccess(true);
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setPredictionTimestamp(LocalDateTime.now());
        
        // 获取预测值
        @SuppressWarnings("unchecked")
        Map<String, Object> predictions = (Map<String, Object>) result.get("predictions");
        
        if (predictions != null) {
            Double dinValue = getNumeric(predictions, "DIN", "din");
            if (dinValue != null) {
                response.setDinLevel(dinValue);
                response.setDinUnit("mg/L");
            }
            Double srpValue = getNumeric(predictions, "SRP", "srp");
            if (srpValue != null) {
                response.setSrpLevel(srpValue);
                response.setSrpUnit("mg/L");
            }
            Double phValue = getNumeric(predictions, "pH", "PH", "ph");
            if (phValue != null) {
                response.setPhLevel(phValue);
                response.setPhUnit("");
            }
            
            // 进行水质等级分类
            try {
                WaterQualityClassificationService.WaterQualityClassification classification = 
                    waterQualityClassificationService.classifyWaterQuality(
                        response.getDinLevel(), 
                        response.getSrpLevel(), 
                        response.getPhLevel()
                    );
                
                // 设置水质等级信息
                response.setWaterQualityLevel(classification.getGrade().getName());
                response.setQualityLevel(classification.getGrade().getDescription());
                
                // 添加额外信息
                Map<String, Object> additionalInfo = new HashMap<>();
                additionalInfo.put("waterQualityGrade", classification.getGrade().getName());
                additionalInfo.put("waterQualityColor", classification.getGrade().getColor());
                additionalInfo.put("waterQualityDescription", classification.getGrade().getDescription());
                additionalInfo.put("waterQualityUsage", classification.getGrade().getUsage());
                additionalInfo.put("classificationReason", classification.getReason());
                additionalInfo.put("overallScore", classification.getOverallScore());

                // 添加时空0.01度范围信息
                Map<String, Double> spatialBounds = new HashMap<>();
                double lat = request.getLatitude();
                double lon = request.getLongitude();
                spatialBounds.put("centerLat", lat);
                spatialBounds.put("centerLon", lon);
                spatialBounds.put("minLat", lat - 0.005);
                spatialBounds.put("maxLat", lat + 0.005);
                spatialBounds.put("minLon", lon - 0.005);
                spatialBounds.put("maxLon", lon + 0.005);
                additionalInfo.put("spatialRange", spatialBounds);
                additionalInfo.put("spatialRangeDescription", "基于经纬度周边0.01度范围的空间平均反演结果");

                // 区域信息与区域平均值（按月份）
                Map<String, Object> regionAverage = computeRegionalAverageWithBox(request);
                if (regionAverage != null) {
                    additionalInfo.put("regionAverage", regionAverage);
                }

                // 数据覆盖提醒
                additionalInfo.put("coverageNotice",
                    "当前CSV命名为 Sentinel2_Reflectance_bohaiYYYY-MM.csv，仅覆盖渤海区域及近年月份；超出范围将返回模型预测或Mock。");
                additionalInfo.put("regionName", getRegionName(request.getLatitude(), request.getLongitude()));
                additionalInfo.put("cityName", getCityName(request.getLatitude(), request.getLongitude()));
                response.setAdditionalInfo(additionalInfo);
                
                logger.info("水质分类完成: {}, 评分: {}",
                    classification.getGrade().getName(), classification.getOverallScore());
                
            } catch (Exception e) {
                logger.warn("水质分类失败，使用默认等级", e);
                response.setWaterQualityLevel("三级");
                response.setQualityLevel("一般");
            }
        }
        
        // 设置置信度
        if (result.containsKey("confidence")) {
            response.setConfidence(((Number) result.get("confidence")).doubleValue());
        }
        
        // 设置模型版本
        if (result.containsKey("modelVersion")) {
            response.setModelVersion((String) result.get("modelVersion"));
        }
        
        // 设置水质等级
        if (result.containsKey("qualityLevel")) {
            response.setWaterQualityLevel((String) result.get("qualityLevel"));
        } else if (result.containsKey("waterQualityLevel")) {
            response.setWaterQualityLevel((String) result.get("waterQualityLevel"));
        } else {
            // 基于DIN值确定水质等级（如果没有提供）
            if (response.getDinLevel() != null) {
                response.setWaterQualityLevel(determineWaterQualityLevel(response.getDinLevel(), "DIN"));
            }
        }
        
        return response;
    }
    
    private Map<String, Object> buildInputDataFromSatellite(SatelliteDataResponse sat, LocalDateTime dt) {
        Map<String, Object> in = new HashMap<>();
        in.put("latitude", sat.getLatitude());
        in.put("longitude", sat.getLongitude());
        if (dt != null) {
            in.put("month", dt.getMonthValue());
            in.put("year", dt.getYear());
        }
        java.util.List<Double> s2Data = new java.util.ArrayList<>();
        // Sentinel-2: B2,B3,B4,B5,B6,B7,B8,B8A 补齐到13
        Map<String, Double> s2 = sat.getS2Data();
        s2Data.add(s2 != null && s2.get("B2") != null ? s2.get("B2") : 0.0);
        s2Data.add(s2 != null && s2.get("B3") != null ? s2.get("B3") : 0.0);
        s2Data.add(s2 != null && s2.get("B4") != null ? s2.get("B4") : 0.0);
        s2Data.add(s2 != null && s2.get("B5") != null ? s2.get("B5") : 0.0);
        s2Data.add(s2 != null && s2.get("B6") != null ? s2.get("B6") : 0.0);
        s2Data.add(s2 != null && s2.get("B7") != null ? s2.get("B7") : 0.0);
        s2Data.add(s2 != null && s2.get("B8") != null ? s2.get("B8") : 0.0);
        s2Data.add(s2 != null && s2.get("B8A") != null ? s2.get("B8A") : 0.0);
        while (s2Data.size() < 13) s2Data.add(0.0);
        in.put("s2Data", s2Data);
        
        java.util.List<Double> s3Data = new java.util.ArrayList<>();
        Map<String, Double> s3 = sat.getS3Data();
        for (int i = 1; i <= 8; i++) {
            String key = String.format("Oa%02d", i);
            s3Data.add(s3 != null && s3.get(key) != null ? s3.get(key) : 0.0);
        }
        while (s3Data.size() < 21) s3Data.add(0.0);
        in.put("s3Data", s3Data);
        
        in.put("chlNN", sat.getChlNN() != null ? sat.getChlNN() : 0.0);
        in.put("tsmNN", sat.getTsmNN() != null ? sat.getTsmNN() : 0.0);
        return in;
    }

    private Double getNumeric(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v == null) continue;
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            try {
                return Double.valueOf(v.toString());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean getBoolean(Map<String, Object> map, String... keys) {
        if (map == null) return false;
        for (String k : keys) {
            Object v = map.get(k);
            if (v == null) continue;
            if (v instanceof Boolean) {
                if ((Boolean) v) return true;
            } else {
                String s = v.toString().trim().toLowerCase();
                if (s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y")) return true;
            }
        }
        return false;
    }

    /**
     * 计算当前点位所在区域的月度区域平均（基于CSV历史数据，若无则返回null）
     */
    private Map<String, Object> computeRegionalAverage(Double latitude, Double longitude, LocalDateTime dateTime) {
        try {
            if (latitude == null || longitude == null) return null;
            // 仅当提供了时间时，按月计算区域平均
            LocalDateTime dt = dateTime != null ? dateTime : LocalDateTime.now();
            LocalDateTime start = dt.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);

            java.util.List<com.oceangpt.model.OceanData> data =
                dataProcessingService.loadHistoricalDataFromCsv(latitude, longitude, start, end);

            if (data == null || data.isEmpty()) {
                Map<String, Object> predictedAvg = computePredictedRegionalAverage(latitude, longitude, dt);
                if (predictedAvg != null) return predictedAvg;
                Map<String, Object> info = new HashMap<>();
                info.put("regionName", getRegionName(latitude, longitude));
                info.put("year", dt.getYear());
                info.put("month", dt.getMonthValue());
                info.put("dataCount", 0);
                info.put("note", "未找到该月份的CSV数据，可能不在渤海覆盖范围或数据未部署。");
                return info;
            }

            // 计算平均值（可用参数：温度、盐度、pH、叶绿素、溶解氧、污染指数）
            double avgTemp = data.stream()
                .filter(d -> d.getSeaSurfaceTemperature() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getSeaSurfaceTemperature)
                .average().orElse(Double.NaN);
            double avgSalinity = data.stream()
                .filter(d -> d.getSalinity() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getSalinity)
                .average().orElse(Double.NaN);
            double avgPh = data.stream()
                .filter(d -> d.getPhLevel() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getPhLevel)
                .average().orElse(Double.NaN);
            double avgChl = data.stream()
                .filter(d -> d.getChlorophyllConcentration() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getChlorophyllConcentration)
                .average().orElse(Double.NaN);

            Map<String, Object> avg = new HashMap<>();
            avg.put("regionName", getRegionName(latitude, longitude));
            avg.put("year", dt.getYear());
            avg.put("month", dt.getMonthValue());
            avg.put("dataCount", data.size());
            avg.put("avgSeaSurfaceTemperature", safeValue(avgTemp));
            avg.put("avgSalinity", safeValue(avgSalinity));
            avg.put("avgPh", safeValue(avgPh));
            avg.put("avgChlorophyll", safeValue(avgChl));
            avg.put("pointLatitude", latitude);
            avg.put("pointLongitude", longitude);
            return avg;
        } catch (Exception e) {
            logger.warn("计算区域平均值失败", e);
            Map<String, Object> predictedAvg = computePredictedRegionalAverage(latitude, longitude, dateTime != null ? dateTime : LocalDateTime.now());
            return predictedAvg;
        }
    }

    private Map<String, Object> computePredictedRegionalAverage(Double latitude, Double longitude, LocalDateTime dt) {
        try {
            double latTol = 0.05;
            double lonTol = 0.05;
            double step = 0.1;
            java.util.List<double[]> grid = new java.util.ArrayList<>();
            for (double lat = latitude - latTol; lat <= latitude + latTol; lat = Math.round((lat + step) * 100.0) / 100.0) {
                for (double lon = longitude - lonTol; lon <= longitude + lonTol; lon = Math.round((lon + step) * 100.0) / 100.0) {
                    grid.add(new double[]{lat, lon});
                }
            }
            int count = 0;
            double sumDin = 0.0, sumSrp = 0.0, sumPh = 0.0;
            for (double[] pt : grid) {
                try {
                    SatelliteDataResponse sat = dataInterpolationService.getInterpolatedSatelliteData(pt[0], pt[1], dt);
                    if (sat == null || !sat.isSuccess()) continue;
                    Map<String, Object> in = buildInputDataFromSatellite(sat, dt);
                    Map<String, Object> res = callPythonModel(in);
                    @SuppressWarnings("unchecked") Map<String, Object> preds = (Map<String, Object>) res.get("predictions");
                    if (preds == null) continue;
                    Double din = getNumeric(preds, "DIN", "din");
                    Double srp = getNumeric(preds, "SRP", "srp");
                    Double ph = getNumeric(preds, "pH", "PH", "ph");
                    if (din == null || srp == null || ph == null) continue;
                    sumDin += din; sumSrp += srp; sumPh += ph; count++;
                } catch (Exception ex) {
                    // 忽略单点失败
                }
            }
            if (count == 0) return null;
            Map<String, Object> avg = new HashMap<>();
            avg.put("regionName", getRegionName(latitude, longitude));
            avg.put("year", dt.getYear());
            avg.put("month", dt.getMonthValue());
            avg.put("gridCount", count);
            avg.put("gridResolutionDeg", step);
            avg.put("cityName", getCityName(latitude, longitude));
            avg.put("avgDIN", sumDin / count);
            avg.put("avgSRP", sumSrp / count);
            avg.put("avgPH", sumPh / count);
            avg.put("note", "基于小范围网格的模型反演平均值");
            return avg;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> computePredictedRegionalAverageBox(Double latitude, Double longitude, LocalDateTime dt, Map<String, Object> params) {
        try {
            double step = 0.1;
            Double res = getNumeric(params != null ? params : new HashMap<>(), "gridResolutionDeg", "resolutionDeg");
            if (res != null && res > 0) step = res;
            double latMin = latitude - 0.05;
            double latMax = latitude + 0.05;
            double lonMin = longitude - 0.05;
            double lonMax = longitude + 0.05;
            Double pLatMin = getNumeric(params != null ? params : new HashMap<>(), "bboxLatMin");
            Double pLatMax = getNumeric(params != null ? params : new HashMap<>(), "bboxLatMax");
            Double pLonMin = getNumeric(params != null ? params : new HashMap<>(), "bboxLonMin");
            Double pLonMax = getNumeric(params != null ? params : new HashMap<>(), "bboxLonMax");
            if (pLatMin != null && pLatMax != null && pLonMin != null && pLonMax != null) {
                latMin = pLatMin; latMax = pLatMax; lonMin = pLonMin; lonMax = pLonMax;
            }
            java.util.List<double[]> grid = new java.util.ArrayList<>();
            for (double lat = latMin; lat <= latMax; lat = Math.round((lat + step) * 100.0) / 100.0) {
                for (double lon = lonMin; lon <= lonMax; lon = Math.round((lon + step) * 100.0) / 100.0) {
                    grid.add(new double[]{lat, lon});
                }
            }
            int count = 0;
            double sumDin = 0.0, sumSrp = 0.0, sumPh = 0.0;
            for (double[] pt : grid) {
                try {
                    SatelliteDataResponse sat = dataInterpolationService.getInterpolatedSatelliteData(pt[0], pt[1], dt);
                    if (sat == null || !sat.isSuccess()) continue;
                    Map<String, Object> in = buildInputDataFromSatellite(sat, dt);
                    Map<String, Object> resMap = callPythonModel(in);
                    @SuppressWarnings("unchecked") Map<String, Object> preds = (Map<String, Object>) resMap.get("predictions");
                    if (preds == null) continue;
                    Double din = getNumeric(preds, "DIN", "din");
                    Double srp = getNumeric(preds, "SRP", "srp");
                    Double ph = getNumeric(preds, "pH", "PH", "ph");
                    if (din == null || srp == null || ph == null) continue;
                    sumDin += din; sumSrp += srp; sumPh += ph; count++;
                } catch (Exception ex) {
                }
            }
            if (count == 0) return null;
            Map<String, Object> avg = new HashMap<>();
            avg.put("regionName", getRegionName(latitude, longitude));
            avg.put("year", dt.getYear());
            avg.put("month", dt.getMonthValue());
            avg.put("gridCount", count);
            avg.put("gridResolutionDeg", step);
            avg.put("cityName", getCityName(latitude, longitude));
            avg.put("avgDIN", sumDin / count);
            avg.put("avgSRP", sumSrp / count);
            avg.put("avgPH", sumPh / count);
            avg.put("note", "基于指定边界的小网格模型反演平均值");
            return avg;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> computeRegionalAverageWithBox(PredictionRequest request) {
        try {
            Map<String, Object> params = request.getAdditionalParams();
            boolean fast = getBoolean(params, "fast", "快速");
            boolean skip = getBoolean(params, "skipRegionalAverage", "noRegionalAverage", "跳过区域平均");
            if (fast || skip) return null;
            Double latitude = request.getLatitude();
            Double longitude = request.getLongitude();
            if (latitude == null || longitude == null) return null;
            LocalDateTime dt = request.getDateTime() != null ? request.getDateTime() : LocalDateTime.now();
            LocalDateTime start = dt.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            java.util.List<com.oceangpt.model.OceanData> data =
                dataProcessingService.loadHistoricalDataFromCsv(latitude, longitude, start, end);
            if (data == null || data.isEmpty()) {
                return computePredictedRegionalAverageBox(latitude, longitude, dt, request.getAdditionalParams());
            }
            double avgTemp = data.stream()
                .filter(d -> d.getSeaSurfaceTemperature() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getSeaSurfaceTemperature)
                .average().orElse(Double.NaN);
            double avgSalinity = data.stream()
                .filter(d -> d.getSalinity() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getSalinity)
                .average().orElse(Double.NaN);
            double avgPh = data.stream()
                .filter(d -> d.getPhLevel() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getPhLevel)
                .average().orElse(Double.NaN);
            double avgChl = data.stream()
                .filter(d -> d.getChlorophyllConcentration() != null)
                .mapToDouble(com.oceangpt.model.OceanData::getChlorophyllConcentration)
                .average().orElse(Double.NaN);
            Map<String, Object> avg = new HashMap<>();
            avg.put("regionName", getRegionName(latitude, longitude));
            avg.put("year", dt.getYear());
            avg.put("month", dt.getMonthValue());
            avg.put("dataCount", data.size());
            avg.put("avgSeaSurfaceTemperature", safeValue(avgTemp));
            avg.put("avgSalinity", safeValue(avgSalinity));
            avg.put("avgPh", safeValue(avgPh));
            avg.put("avgChlorophyll", safeValue(avgChl));
            avg.put("pointLatitude", latitude);
            avg.put("pointLongitude", longitude);
            return avg;
        } catch (Exception e) {
            return computePredictedRegionalAverageBox(request.getLatitude(), request.getLongitude(), request.getDateTime() != null ? request.getDateTime() : LocalDateTime.now(), request.getAdditionalParams());
        }
    }

    private Object safeValue(double v) {
        return Double.isNaN(v) ? null : v;
    }

    /**
     * 简单区域识别（当前以渤海矩形近似）
     */
    private String getRegionName(Double lat, Double lon) {
        if (lat == null || lon == null) return "未知区域";
        // 渤海近似边界：纬度[37.0, 41.5]，经度[117.0, 121.8]
        boolean inBohai = lat >= 37.0 && lat <= 41.5 && lon >= 117.0 && lon <= 121.8;
        return inBohai ? "渤海" : "非渤海范围";
    }
    
    /**
     * 根据预测值确定水质等级
     */
    private String determineWaterQualityLevel(Double value, String targetType) {
        if (value == null) return "UNKNOWN";
        
        switch (targetType) {
            case "DIN":
                if (value < 0.02) return "EXCELLENT";
                if (value < 0.05) return "GOOD";
                if (value < 0.1) return "MODERATE";
                return "POOR";
                
            case "SRP":
                if (value < 0.005) return "EXCELLENT";
                if (value < 0.01) return "GOOD";
                if (value < 0.02) return "MODERATE";
                return "POOR";
                
            case "pH":
                if (value >= 7.5 && value <= 8.5) return "EXCELLENT";
                if (value >= 7.0 && value <= 9.0) return "GOOD";
                if (value >= 6.5 && value <= 9.5) return "MODERATE";
                return "POOR";
                
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * 创建错误响应
     */
    private PredictionResponse createErrorResponse(String errorMessage, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setPredictionTimestamp(LocalDateTime.now());
        return response;
    }
    
    private String getCityName(Double lat, Double lon) {
        if (lat == null || lon == null) return "未知城市";
        java.util.List<Object[]> cities = java.util.Arrays.asList(
            new Object[]{"天津", 39.125, 117.190},
            new Object[]{"唐山", 39.635, 118.180},
            new Object[]{"秦皇岛", 39.935, 119.600},
            new Object[]{"大连", 38.913, 121.614},
            new Object[]{"烟台", 37.463, 121.447},
            new Object[]{"青岛", 36.067, 120.382}
        );
        String best = "未知城市";
        double bestDist = Double.MAX_VALUE;
        for (Object[] c : cities) {
            double clat = (Double) c[1];
            double clon = (Double) c[2];
            double dlat = lat - clat;
            double dlon = lon - clon;
            double dist = dlat * dlat + dlon * dlon;
            if (dist < bestDist) {
                bestDist = dist;
                best = (String) c[0];
            }
        }
        return best;
    }
}
