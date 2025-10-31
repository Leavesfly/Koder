package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.tool.Tool;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具命令
 * 列出所有可用工具
 */
@Component
@RequiredArgsConstructor
public class ToolsCommand implements Command {

    private final ToolExecutor toolExecutor;

    @Override
    public String getName() {
        return "tools";
    }

    @Override
    public String getDescription() {
        return "列出所有可用工具";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        List<Tool<?, ?>> tools = toolExecutor.getAllTools();

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== 可用工具列表 ===\n\n");

        // 按类别分组显示
        sb.append("文件操作:\n");
        tools.stream()
                .filter(t -> t.getName().contains("View") || 
                           t.getName().contains("Edit") || 
                           t.getName().contains("Create"))
                .forEach(t -> sb.append(String.format("  %-15s - %s\n", 
                        t.getName(), t.getDescription())));

        sb.append("\n搜索工具:\n");
        tools.stream()
                .filter(t -> t.getName().contains("Glob") || 
                           t.getName().contains("Grep") || 
                           t.getName().contains("List"))
                .forEach(t -> sb.append(String.format("  %-15s - %s\n", 
                        t.getName(), t.getDescription())));

        sb.append("\n系统工具:\n");
        tools.stream()
                .filter(t -> t.getName().equals("Bash"))
                .forEach(t -> sb.append(String.format("  %-15s - %s\n", 
                        t.getName(), t.getDescription())));

        sb.append("\nAI辅助:\n");
        tools.stream()
                .filter(t -> t.getName().contains("Think") || 
                           t.getName().contains("Task") || 
                           t.getName().contains("Expert"))
                .forEach(t -> sb.append(String.format("  %-15s - %s\n", 
                        t.getName(), t.getDescription())));

        sb.append("\n总计: ").append(tools.size()).append(" 个工具\n");

        context.getOutput().println(sb.toString());
        return CommandResult.success("");
    }
}
