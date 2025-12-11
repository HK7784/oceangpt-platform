package com.oceangpt.agent;

import java.util.Map;

/**
 * Agent可调用工具的标准接口
 * 参考LangGraph/MetaGPT风格的工具抽象
 */
public interface AgentTool {
    String getName();
    String getDescription();
    Map<String, Object> invoke(Map<String, Object> input) throws Exception;
}
