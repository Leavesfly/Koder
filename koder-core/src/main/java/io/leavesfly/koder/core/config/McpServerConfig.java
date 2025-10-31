package io.leavesfly.koder.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP服务器配置基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class McpServerConfig {
    
    /**
     * 传输类型
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * 环境变量配置（可选）
     */
    @JsonProperty("env")
    private Map<String, String> env;
}

/**
 * Stdio传输方式的MCP服务器配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class McpStdioServerConfig extends McpServerConfig {
    
    /**
     * 启动命令
     */
    @JsonProperty("command")
    private String command;
    
    /**
     * 命令参数
     */
    @JsonProperty("args")
    private List<String> args;
}

/**
 * SSE传输方式的MCP服务器配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class McpSSEServerConfig extends McpServerConfig {
    
    /**
     * SSE服务器URL
     */
    @JsonProperty("url")
    private String url;
}
