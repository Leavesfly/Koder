package io.leavesfly.koder.mcp.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP服务器配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPServerConfig {

    /**
     * 服务器名称
     */
    private String name;

    /**
     * 传输类型（stdio或sse）
     */
    private String type;

    /**
     * 命令（stdio模式）
     */
    private String command;

    /**
     * 参数（stdio模式）
     */
    private String[] args;

    /**
     * 环境变量（stdio模式）
     */
    private Map<String, String> env;

    /**
     * URL（sse模式）
     */
    private String url;

    /**
     * 是否已批准
     */
    private boolean approved;

    /**
     * 配置范围（project/global/mcprc）
     */
    private String scope;
}
