package com.oceangpt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OceanGPT模型配置类
 */
@Configuration
@ConfigurationProperties(prefix = "oceangpt.model")
public class ModelConfig {
    
    private String path = "models/oceangpt-14b.zip";
    private int batchSize = 32;
    private int maxSequenceLength = 512;
    private int inferenceThreads = 4;
    private boolean mockMode = false;
    
    // Getters and Setters
    public String getPath() {
        return path;
    }
    
    public String getModelPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getMaxSequenceLength() {
        return maxSequenceLength;
    }
    
    public void setMaxSequenceLength(int maxSequenceLength) {
        this.maxSequenceLength = maxSequenceLength;
    }
    
    public int getInferenceThreads() {
        return inferenceThreads;
    }
    
    public void setInferenceThreads(int inferenceThreads) {
        this.inferenceThreads = inferenceThreads;
    }
    
    public boolean isMockMode() {
        return mockMode;
    }
    
    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }
}