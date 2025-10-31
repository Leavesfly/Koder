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
 * logout命令 - 用户登出
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutCommand implements Command {

    private final ConfigManager configManager;

    @Override
    public String getName() {
        return "logout";
    }

    @Override
    public String getDescription() {
        return "退出登录";
    }

    @Override
    public String getUsage() {
        return "/logout";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        GlobalConfig config = configManager.getGlobalConfig();
        
        if (config.getOauthAccount() == null) {
            return CommandResult.success("\n当前未登录任何账户\n");
        }
        
        StringBuilder output = new StringBuilder();
        output.append("\n=== 🚪 退出登录 ===\n\n");
        
        // 显示当前账户信息
        output.append("当前已登录\n\n");
        
        output.append("确认退出登录？这将：\n");
        output.append("  • 清除账户信息\n");
        output.append("  • 重置引导状态\n");
        output.append("  • 清除已批准的自定义 API Key\n\n");
        output.append("是否继续？(y/n): ");
        
        context.getOutput().println(output.toString());
        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("> ");
            String response = scanner.nextLine().trim().toLowerCase();
            
            if (!response.equals("y") && !response.equals("yes")) {
                return CommandResult.success("\n已取消退出登录\n");
            }
        }
        
        // 执行登出操作
        config.setOauthAccount(null);
        config.setHasCompletedOnboarding(false);
        
        // 清除已批准的自定义 API Key
        if (config.getCustomApiKeyResponses() != null) {
            config.getCustomApiKeyResponses().clear();
        }
        
        // 保存配置
        configManager.saveGlobalConfig();
        
        log.info("用户已登出");
        
        output.setLength(0);
        output.append("\n✅ 已成功退出登录\n\n");
        output.append("提示：\n");
        output.append("  • 使用 /login 重新登录\n");
        output.append("  • 使用 /config 查看当前配置\n");
        
        return CommandResult.success(output.toString());
    }
}
