package io.leavesfly.koder.mcp.wrapper;

import io.leavesfly.koder.mcp.client.MCPClient;
import io.leavesfly.koder.mcp.client.MCPClientManager;
import io.leavesfly.koder.mcp.protocol.MCPTool;
import io.leavesfly.koder.tool.AbstractTool;
import io.leavesfly.koder.tool.ToolResponse;
import io.leavesfly.koder.tool.ToolUseContext;
import io.leavesfly.koder.tool.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * MCP工具包装器
 * 将MCP工具适配为Koder内部工具接口
 */
@Slf4j
public class MCPToolWrapper extends AbstractTool<Map<String, Object>, Map<String, Object>> {

    private final MCPTool mcpTool;
    private final String serverName;
    private final MCPClientManager clientManager;

    public MCPToolWrapper(MCPTool mcpTool, String serverName, MCPClientManager clientManager) {
        this.mcpTool = mcpTool;
        this.serverName = serverName;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "mcp__" + serverName + "__" + mcpTool.getName();
    }

    @Override
    public String getDescription() {
        return String.format("[MCP:%s] %s", serverName, mcpTool.getDescription());
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return mcpTool.getInputSchema();
    }

    @Override
    public boolean isReadOnly() {
        // MCP工具的只读性由工具本身决定
        // 这里保守起见默认为false
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        // MCP工具的并发安全性由工具本身决定
        return false;
    }

    @Override
    public ValidationResult validateInput(Map<String, Object> input, ToolUseContext context) {
        // MCP工具通过JSON Schema自行验证
        // 这里简单检查input非空
        if (input == null) {
            return ValidationResult.builder()
                    .result(false)
                    .message("输入参数不能为空")
                    .build();
        }

        return ValidationResult.builder()
                .result(true)
                .build();
    }

    @Override
    public Flux<ToolResponse<Map<String, Object>>> call(
            Map<String, Object> input,
            ToolUseContext context) {

        return Flux.create(sink -> {
            clientManager.getClient(serverName)
                    .flatMap(client -> client.callTool(mcpTool.getName(), input))
                    .subscribe(
                            result -> {
                                // 发送结果
                                sink.next(ToolResponse.result(result));
                                sink.complete();
                            },
                            error -> {
                                log.error("调用MCP工具失败: {} ({})", getName(), error.getMessage());
                                sink.error(error);
                            }
                    );
        });
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input, boolean verbose) {
        if (verbose) {
            return String.format("MCP Tool [%s]: %s", serverName, mcpTool.getName());
        }
        return mcpTool.getName();
    }
    
    @Override
    public String renderToolResultMessage(Map<String, Object> output) {
        return String.format("Result from %s", mcpTool.getName());
    }

    /**
     * 获取MCP服务器名称
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * 获取原始MCP工具
     */
    public MCPTool getMcpTool() {
        return mcpTool;
    }
}
