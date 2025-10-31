package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.mcp.client.MCPClient;
import io.leavesfly.koder.mcp.client.MCPClientManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mcp命令 - 显示MCP服务器连接状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPCommand implements Command {

    private final MCPClientManager mcpClientManager;

    @Override
    public String getName() {
        return "mcp";
    }

    @Override
    public String getDescription() {
        return "显示MCP服务器连接状态";
    }

    @Override
    public String getUsage() {
        return "/mcp";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        StringBuilder output = new StringBuilder();
        output.append("\n=== MCP服务器状态 ===\n\n");

        // 获取所有配置
        var serverConfigs = mcpClientManager.getAllServerConfigs();
        
        if (serverConfigs.isEmpty()) {
            output.append("暂无配置的MCP服务器\n\n");
            output.append("提示: 在配置文件中添加MCP服务器配置\n");
            return CommandResult.success(output.toString());
        }

        // 获取已连接的客户端
        var clients = mcpClientManager.getAllClients();
        
        // 显示每个服务器的状态
        for (var config : serverConfigs) {
            String serverName = config.getName();
            var client = clients.get(serverName);
            
            boolean isConnected = client != null && client.isConnected();
            String status = isConnected ? "✅ 已连接" : "❌ 未连接";
            
            output.append("  • ").append(serverName).append(": ").append(status);
            output.append(" (").append(config.getType()).append(")");
            output.append("\n");
            
            // 显示可用工具数量
            if (isConnected) {
                try {
                    var tools = client.listTools().block();
                    if (tools != null) {
                        output.append("    - ").append(tools.size()).append("个工具\n");
                    }
                } catch (Exception e) {
                    output.append("    - 工具列表获取失败\n");
                }
            }
        }
        
        output.append("\n总计: ").append(serverConfigs.size()).append(" 个服务器");
        output.append(", ").append(clients.size()).append(" 个已连接\n");

        return CommandResult.success(output.toString());
    }
}
