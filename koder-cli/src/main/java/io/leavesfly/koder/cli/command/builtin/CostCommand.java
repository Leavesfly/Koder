package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.cli.repl.REPLSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * cost命令 - 显示当前会话的token消耗和成本
 */
@Component
@RequiredArgsConstructor
public class CostCommand implements Command {

    private final REPLSession session;

    @Override
    public String getName() {
        return "cost";
    }

    @Override
    public String getDescription() {
        return "显示当前会话的token消耗和成本";
    }

    @Override
    public String getUsage() {
        return "/cost";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        StringBuilder output = new StringBuilder();
        output.append("\n=== 会话成本统计 ===\n\n");

        // 获取成本追踪器
        var costTracker = session.getCostTracker();
        
        // 显示Token使用和成本
        output.append(costTracker.formatCostReport());
        
        // 显示会话信息
        output.append("\n会话信息:\n");
        output.append("消息数量: ").append(session.getMessages().size()).append("\n");
        output.append("会话时长: ").append(formatDuration(session.getStartTime())).append("\n");

        return CommandResult.success(output.toString());
    }

    private String formatDuration(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}
