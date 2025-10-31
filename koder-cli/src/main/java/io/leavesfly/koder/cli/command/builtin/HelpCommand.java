package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import org.springframework.stereotype.Component;

/**
 * 帮助命令
 * 显示所有可用命令的列表和说明
 */
@Component
public class HelpCommand implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "显示帮助信息";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Koder 可用命令 ===\n\n");

        sb.append("基本命令:\n");
        sb.append("  /help       - 显示此帮助信息\n");
        sb.append("  /version    - 显示版本信息\n");
        sb.append("  /clear      - 清空屏幕\n");
        sb.append("  /exit       - 退出CLI\n\n");

        sb.append("配置命令:\n");
        sb.append("  /config     - 管理配置设置\n");
        sb.append("  /model      - 管理AI模型\n\n");

        sb.append("工具命令:\n");
        sb.append("  /tools      - 列出可用工具\n");
        sb.append("  /agents     - 管理智能代理\n");
        sb.append("  /mcp        - 查看MCP服务器状态\n\n");

        sb.append("会话命令:\n");
        sb.append("  /cost       - 查看会话成本统计\n");
        sb.append("  /compact    - 压缩对话历史\n");
        sb.append("  /resume     - 恢复之前的对话\n\n");

        sb.append("高级命令:\n");
        sb.append("  /doctor     - 检查系统健康状态\n");
        sb.append("  /listen     - 启动语音输入模式\n\n");

        sb.append("使用技巧:\n");
        sb.append("  - 直接输入问题与AI对话\n");
        sb.append("  - 使用Ctrl+C取消当前操作\n");
        sb.append("  - 使用Ctrl+D或输入/exit退出\n");

        context.getOutput().println(sb.toString());

        return CommandResult.success("");
    }
}
