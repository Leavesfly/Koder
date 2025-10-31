package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import org.springframework.stereotype.Component;

/**
 * 清屏命令
 */
@Component
public class ClearCommand implements Command {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "清空屏幕和对话历史";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        return CommandResult.builder()
                .success(true)
                .shouldClearScreen(true)
                .message("已清空屏幕")
                .build();
    }
}
