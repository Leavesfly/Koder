package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import org.springframework.stereotype.Component;

/**
 * 退出命令
 */
@Component
public class ExitCommand implements Command {

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String getDescription() {
        return "退出程序";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.getOutput().success("\n感谢使用Koder！再见！\n");
        return CommandResult.exit();
    }
}
