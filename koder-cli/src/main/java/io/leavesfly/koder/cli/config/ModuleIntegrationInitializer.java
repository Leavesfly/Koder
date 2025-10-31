package io.leavesfly.koder.cli.config;

import io.leavesfly.koder.agent.AgentRegistry;
import io.leavesfly.koder.cli.command.CommandRegistry;
import io.leavesfly.koder.mcp.client.MCPClientManager;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 模块集成初始化器
 * 负责在应用启动时初始化和集成所有模块
 */
@Slf4j
@Component
@Order(1) // 第一个运行，在其他初始化器之前
@RequiredArgsConstructor
public class ModuleIntegrationInitializer implements CommandLineRunner {

    private final ToolExecutor toolExecutor;
    private final AgentRegistry agentRegistry;
    private final MCPClientManager mcpClientManager;
    private final CommandRegistry commandRegistry;

    @Override
    public void run(String... args) throws Exception {
        log.info("====================================");
        log.info("开始初始化Koder模块集成...");
        log.info("====================================");

        // 1. 初始化工具系统
        initializeToolSystem();

        // 2. 初始化代理系统
        initializeAgentSystem();

        // 3. 初始化MCP客户端
        initializeMCPSystem();

        // 4. 初始化命令系统
        initializeCommandSystem();

        // 5. 验证集成
        verifyIntegration();

        log.info("====================================");
        log.info("Koder模块集成初始化完成！");
        log.info("====================================\n");
    }

    /**
     * 初始化工具系统
     */
    private void initializeToolSystem() {
        log.info("→ 初始化工具系统...");
        
        int toolCount = toolExecutor.getAllTools().size();
        log.info("  ✓ 已注册 {} 个工具", toolCount);
        
        if (log.isDebugEnabled()) {
            toolExecutor.getAllTools().forEach(tool -> 
                log.debug("    - {} ({})", tool.getName(), tool.getDescription())
            );
        }
    }

    /**
     * 初始化代理系统
     */
    private void initializeAgentSystem() {
        log.info("→ 初始化代理系统...");
        
        try {
            agentRegistry.initialize();
            int agentCount = agentRegistry.getAllAgents().size();
            log.info("  ✓ 已加载 {} 个代理", agentCount);
            
            if (log.isDebugEnabled()) {
                agentRegistry.getAllAgents().forEach(agent -> 
                    log.debug("    - {} ({}): {}", 
                        agent.getAgentType(), 
                        agent.getLocation(), 
                        agent.getSystemPrompt().substring(0, Math.min(50, agent.getSystemPrompt().length())) + "..."
                    )
                );
            }
        } catch (Exception e) {
            log.warn("  ! 代理系统初始化失败: {}", e.getMessage());
            log.debug("详细错误", e);
        }
    }

    /**
     * 初始化MCP系统
     */
    private void initializeMCPSystem() {
        log.info("→ 初始化MCP客户端系统...");
        
        try {
            mcpClientManager.initialize();
            int serverCount = mcpClientManager.getAllClients().size();
            log.info("  ✓ 已配置 {} 个MCP服务器", serverCount);
            
            if (log.isDebugEnabled() && serverCount > 0) {
                mcpClientManager.getAllClients().keySet().forEach(serverName -> 
                    log.debug("    - {}", serverName)
                );
            }
        } catch (Exception e) {
            log.warn("  ! MCP系统初始化失败: {}", e.getMessage());
            log.debug("详细错误", e);
        }
    }

    /**
     * 初始化命令系统
     */
    private void initializeCommandSystem() {
        log.info("→ 初始化命令系统...");
        
        int commandCount = commandRegistry.getAllCommands().size();
        log.info("  ✓ 已注册 {} 个命令", commandCount);
        
        if (log.isDebugEnabled()) {
            commandRegistry.getAllCommands().forEach(command -> 
                log.debug("    - /{} - {}", command.getName(), command.getDescription())
            );
        }
    }

    /**
     * 验证集成
     */
    private void verifyIntegration() {
        log.info("→ 验证模块集成...");
        
        boolean allOk = true;

        // 验证工具系统
        if (toolExecutor.getAllTools().isEmpty()) {
            log.warn("  ! 警告: 未注册任何工具");
            allOk = false;
        }

        // 验证命令系统
        if (commandRegistry.getAllCommands().isEmpty()) {
            log.error("  ✗ 错误: 未注册任何命令");
            allOk = false;
        }

        // 检查关键命令
        String[] essentialCommands = {"help", "exit", "model", "agents"};
        for (String cmdName : essentialCommands) {
            if (commandRegistry.find(cmdName).isEmpty()) {
                log.warn("  ! 警告: 未找到关键命令: /{}", cmdName);
                allOk = false;
            }
        }

        if (allOk) {
            log.info("  ✓ 所有模块集成验证通过");
        } else {
            log.warn("  ! 部分模块集成存在问题，但系统可以继续运行");
        }
    }
}
