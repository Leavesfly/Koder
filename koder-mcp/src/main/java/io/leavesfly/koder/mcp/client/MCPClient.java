package io.leavesfly.koder.mcp.client;

import io.leavesfly.koder.mcp.config.MCPServerConfig;
import io.leavesfly.koder.mcp.protocol.MCPRequest;
import io.leavesfly.koder.mcp.protocol.MCPResponse;
import io.leavesfly.koder.mcp.protocol.MCPTool;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP客户端接口
 * 统一Stdio和SSE传输方式
 */
public interface MCPClient {

    /**
     * 连接到服务器
     */
    Mono<Void> connect();

    /**
     * 断开连接
     */
    Mono<Void> disconnect();

    /**
     * 发送请求
     */
    Mono<MCPResponse> sendRequest(MCPRequest request);

    /**
     * 列出可用工具
     */
    Mono<List<MCPTool>> listTools();

    /**
     * 调用工具
     */
    Mono<Map<String, Object>> callTool(String toolName, Map<String, Object> arguments);

    /**
     * 获取服务器配置
     */
    MCPServerConfig getConfig();

    /**
     * 是否已连接
     */
    boolean isConnected();
}
