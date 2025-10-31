package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * bug命令 - 提交反馈
 */
@Slf4j
@Component
public class BugCommand implements Command {

    private static final String GITHUB_ISSUES_URL = "https://github.com/yourusername/koder/issues/new";
    private static final String PRODUCT_NAME = "Koder";

    @Override
    public String getName() {
        return "bug";
    }

    @Override
    public String getDescription() {
        return "提交问题反馈或功能建议";
    }

    @Override
    public String getUsage() {
        return "/bug [描述]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String[] args = context.getArgs().toArray(new String[0]);
        
        StringBuilder output = new StringBuilder();
        output.append("\n=== 📝 问题反馈 ===\n\n");
        
        if (args.length > 0) {
            // 如果提供了描述，显示反馈内容
            String description = String.join(" ", args);
            output.append("感谢您的反馈！\n\n");
            output.append("您的问题/建议：\n");
            output.append("─────────────────────\n");
            output.append(description).append("\n");
            output.append("─────────────────────\n\n");
        }
        
        output.append("💡 提交反馈的方式：\n\n");
        output.append("1. GitHub Issues（推荐）\n");
        output.append(String.format("   访问: %s\n\n", GITHUB_ISSUES_URL));
        
        output.append("2. 包含以下信息会很有帮助：\n");
        output.append("   • 问题描述或功能建议\n");
        output.append("   • 复现步骤（如果是Bug）\n");
        output.append("   • 期望的行为\n");
        output.append("   • 系统环境信息\n\n");
        
        output.append("3. 获取系统信息：\n");
        output.append("   • 版本: 使用 /version 查看\n");
        output.append("   • 配置: 使用 /config 查看\n");
        output.append("   • 诊断: 使用 /doctor 检查环境\n\n");
        
        output.append(String.format("感谢使用 %s！您的反馈帮助我们变得更好。\n", PRODUCT_NAME));
        
        return CommandResult.success(output.toString());
    }
}
