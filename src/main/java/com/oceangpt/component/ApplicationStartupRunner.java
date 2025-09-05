package com.oceangpt.component;

import com.oceangpt.config.ModelConfig;
import com.oceangpt.service.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用程序启动运行器
 * 在应用启动时执行初始化检查和设置
 */
@Component
public class ApplicationStartupRunner implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupRunner.class);
    
    @Autowired
    private ModelConfig modelConfig;
    
    @Autowired
    private ModelService modelService;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("=== OceanGPT 海水水质监测系统启动检查 ===");
        
        // 1. 检查系统环境
        checkSystemEnvironment();
        
        // 2. 验证模型文件
        verifyModelFile();
        
        // 3. 检查目录结构
        checkDirectoryStructure();
        
        // 4. 初始化模型服务
        initializeModelService();
        
        // 5. 系统就绪检查
        performReadinessCheck();
        
        logger.info("=== OceanGPT 系统启动完成 ===");
    }
    
    /**
     * 检查系统环境
     */
    private void checkSystemEnvironment() {
        logger.info("检查系统环境...");
        
        // 检查Java版本
        String javaVersion = System.getProperty("java.version");
        logger.info("Java版本: {}", javaVersion);
        
        // 检查可用内存
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        logger.info("内存信息 - 最大: {}MB, 总计: {}MB, 可用: {}MB", 
                   maxMemory / 1024 / 1024, 
                   totalMemory / 1024 / 1024, 
                   freeMemory / 1024 / 1024);
        
        // 检查是否有足够内存运行模型
        if (maxMemory < 2L * 1024 * 1024 * 1024) { // 2GB
            logger.warn("警告: 可用内存可能不足以运行OceanGPT模型，建议至少分配2GB内存");
        }
        
        // 检查CPU核心数
        int processors = runtime.availableProcessors();
        logger.info("CPU核心数: {}", processors);
        
        // 检查操作系统
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        logger.info("操作系统: {} ({})", osName, osArch);
        
        // 检查GPU支持（简单检查）
        checkGpuSupport();
    }
    
    /**
     * 验证模型文件
     */
    private void verifyModelFile() {
        logger.info("验证模型文件...");
        
        String modelPath = modelConfig.getModelPath();
        logger.info("模型文件路径: {}", modelPath);
        
        if (modelPath == null || modelPath.trim().isEmpty()) {
            logger.error("模型文件路径未配置");
            return;
        }
        
        Path path = Paths.get(modelPath);
        
        if (!Files.exists(path)) {
            logger.warn("模型文件不存在: {}", modelPath);
            logger.info("将使用模拟模型进行演示");
            return;
        }
        
        try {
            long fileSize = Files.size(path);
            logger.info("模型文件大小: {}MB", fileSize / 1024 / 1024);
            
            // 检查文件是否可读
            if (!Files.isReadable(path)) {
                logger.error("模型文件不可读: {}", modelPath);
                return;
            }
            
            // 检查文件格式（基于扩展名）
            String fileName = path.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".onnx") || fileName.endsWith(".pb") || 
                fileName.endsWith(".h5") || fileName.endsWith(".zip")) {
                logger.info("模型文件格式验证通过");
            } else {
                logger.warn("未识别的模型文件格式: {}", fileName);
            }
            
        } catch (Exception e) {
            logger.error("验证模型文件时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 检查目录结构
     */
    private void checkDirectoryStructure() {
        logger.info("检查目录结构...");
        
        String[] requiredDirs = {
            "models",
            "logs",
            "data",
            "temp"
        };
        
        for (String dirName : requiredDirs) {
            Path dirPath = Paths.get(dirName);
            
            if (!Files.exists(dirPath)) {
                try {
                    Files.createDirectories(dirPath);
                    logger.info("创建目录: {}", dirName);
                } catch (Exception e) {
                    logger.error("创建目录失败 {}: {}", dirName, e.getMessage());
                }
            } else {
                logger.debug("目录已存在: {}", dirName);
            }
        }
    }
    
    /**
     * 初始化模型服务
     */
    private void initializeModelService() {
        logger.info("初始化模型服务...");
        
        try {
            // 尝试初始化模型
            boolean initialized = modelService.tryInitializeModel();
            
            if (initialized) {
                logger.info("模型服务初始化成功");
                
                // 获取模型信息
                var modelInfo = modelService.getModelInfo();
                logger.info("模型信息: {}", modelInfo);
                
            } else {
                logger.warn("模型服务初始化失败，将使用模拟模式");
            }
            
        } catch (Exception e) {
            logger.error("初始化模型服务时发生错误: {}", e.getMessage(), e);
            logger.info("系统将在模拟模式下运行");
        }
    }
    
    /**
     * 执行系统就绪检查
     */
    private void performReadinessCheck() {
        logger.info("执行系统就绪检查...");
        
        boolean systemReady = true;
        
        // 检查模型服务状态
        try {
            boolean modelReady = modelService.isModelReady();
            logger.info("模型服务状态: {}", modelReady ? "就绪" : "未就绪");
            
            if (!modelReady) {
                logger.warn("模型服务未就绪，某些功能可能受限");
            }
            
        } catch (Exception e) {
            logger.error("检查模型服务状态失败: {}", e.getMessage());
            systemReady = false;
        }
        
        // 检查数据库连接
        // 这里可以添加数据库连接检查逻辑
        
        // 检查外部API连接
        // 这里可以添加NOAA API连接检查逻辑
        
        if (systemReady) {
            logger.info("✅ 系统就绪检查通过");
        } else {
            logger.warn("⚠️ 系统就绪检查发现问题，请检查日志");
        }
        
        // 打印系统配置摘要
        printSystemSummary();
    }
    
    /**
     * 检查GPU支持
     */
    private void checkGpuSupport() {
        try {
            // 检查CUDA相关环境变量
            String cudaPath = System.getenv("CUDA_PATH");
            String cudaHome = System.getenv("CUDA_HOME");
            
            if (cudaPath != null || cudaHome != null) {
                logger.info("检测到CUDA环境: CUDA_PATH={}, CUDA_HOME={}", cudaPath, cudaHome);
            } else {
                logger.info("未检测到CUDA环境，将使用CPU进行推理");
            }
            
            // 检查ND4J后端
            String nd4jBackend = System.getProperty("org.nd4j.linalg.factory");
            if (nd4jBackend != null) {
                logger.info("ND4J后端: {}", nd4jBackend);
            }
            
        } catch (Exception e) {
            logger.debug("检查GPU支持时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 打印系统配置摘要
     */
    private void printSystemSummary() {
        logger.info("\n" +
                   "=== OceanGPT 系统配置摘要 ===\n" +
                   "模型路径: {}\n" +
                   "批处理大小: {}\n" +
                   "最大序列长度: {}\n" +
                   "推理线程数: {}\n" +
                   "系统状态: 运行中\n" +
                   "API端点: http://localhost:8080/api/v1\n" +
                   "健康检查: http://localhost:8080/api/v1/actuator/health\n" +
                   "API文档: http://localhost:8080/api/v1/swagger-ui.html\n" +
                   "==============================",
                   modelConfig.getModelPath(),
                   modelConfig.getBatchSize(),
                   modelConfig.getMaxSequenceLength(),
                   modelConfig.getInferenceThreads());
    }
    
    /**
     * 创建示例模型文件（用于演示）
     */
    private void createSampleModelFile() {
        try {
            String modelPath = modelConfig.getModelPath();
            if (modelPath != null && !modelPath.trim().isEmpty()) {
                Path path = Paths.get(modelPath);
                Path parentDir = path.getParent();
                
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                
                if (!Files.exists(path)) {
                    // 创建一个空的示例文件
                    Files.createFile(path);
                    logger.info("创建示例模型文件: {}", modelPath);
                    logger.warn("这是一个空的示例文件，请替换为实际的OceanGPT模型文件");
                }
            }
        } catch (Exception e) {
            logger.error("创建示例模型文件失败: {}", e.getMessage());
        }
    }
}