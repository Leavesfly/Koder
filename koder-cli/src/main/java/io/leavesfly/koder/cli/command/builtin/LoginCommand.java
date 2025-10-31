package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.core.config.GlobalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * login命令 - 用户登录
 * 
 * 注意：这是一个简化实现
 * 完整的 OAuth 流程需要集成 Anthropic OAuth API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginCommand implements Command {

    private final ConfigManager configManager;

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public String getDescription() {
        GlobalConfig config = configManager.getGlobalConfig();
        boolean isLoggedIn = config.getOauthAccount() != null;
        return isLoggedIn ? "切换账户" : "登录到您的账户";
    }

    @Override
    public String getUsage() {
        return "/login";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        GlobalConfig config = configManager.getGlobalConfig();
        boolean alreadyLoggedIn = config.getOauthAccount() != null;
        
        StringBuilder output = new StringBuilder();
        output.append("\n=== 🔐 用户登录 ===\n\n");
        
        if (alreadyLoggedIn) {
            output.append("当前已登录账户");
            // 注意：AccountInfo 是包级别可见，无法直接访问其字段
            // 需要通过 GlobalConfig 提供的公共方法访问
            output.append("\n\n是否切换账户？(y/n): ");
            context.getOutput().println(output.toString());
            
            try (Scanner scanner = new Scanner(System.in)) {
                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    return CommandResult.success("取消切换账户");
                }
            }
        }
        
        output.setLength(0);
        output.append("\n登录方式：\n\n");
        output.append("1. OAuth 登录（推荐）\n");
        output.append("   - 浏览器授权登录\n");
        output.append("   - 安全且便捷\n\n");
        
        output.append("2. API Key 登录\n");
        output.append("   - 直接配置 API Key\n");
        output.append("   - 适合自动化场景\n\n");
        
        output.append("注意：\n");
        output.append("- 完整的 OAuth 流程需要启动本地服务器接收回调\n");
        output.append("- 当前简化实现，建议通过配置文件设置认证信息\n");
        output.append("- 配置文件位置: ~/.koder.json\n\n");
        
        output.append("配置示例：\n");
        output.append("{\n");
        output.append("  \"oauthAccount\": {\n");
        output.append("    \"emailAddress\": \"user@example.com\",\n");
        output.append("    \"accountUuid\": \"your-account-uuid\"\n");
        output.append("  }\n");
        output.append("}\n\n");
        
        output.append("提示：使用 /config 命令查看当前配置\n");
        
        return CommandResult.success(output.toString());
    }
}
