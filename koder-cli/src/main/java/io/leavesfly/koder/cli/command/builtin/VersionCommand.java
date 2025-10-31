package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * version命令 - 显示Koder版本信息
 */
@Component
@RequiredArgsConstructor
public class VersionCommand implements Command {

    private static final String VERSION = "1.0.0-SNAPSHOT";
    private static final String BUILD_DATE = "2025-01-29";

    @Override
    public String getName() {
        return "version";
    }

    @Override
    public String getDescription() {
        return "显示Koder版本信息";
    }

    @Override
    public String getUsage() {
        return "/version";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        StringBuilder output = new StringBuilder();
        output.append("\n");
        output.append("Koder AI编程助手 (Java版)\n");
        output.append("版本: ").append(VERSION).append("\n");
        output.append("构建日期: ").append(BUILD_DATE).append("\n");
        output.append("Java版本: ").append(System.getProperty("java.version")).append("\n");
        output.append("\n");

        return CommandResult.success(output.toString());
    }
}
