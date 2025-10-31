package io.leavesfly.koder.cli.command.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.core.config.GlobalConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 配置命令
 * 查看和修改配置
 */
@Component
@RequiredArgsConstructor
public class ConfigCommand implements Command {

    private final ConfigManager configManager;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "查看或修改配置";
    }

    @Override
    public String getUsage() {
        return "/config [键] [值]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.getArgs().isEmpty()) {
            // 显示所有配置
            return showAllConfig(context);
        } else if (context.getArgs().size() == 1) {
            // 显示指定配置项
            String key = context.getArgs().get(0);
            return showConfig(context, key);
        } else {
            // 设置配置项
            String key = context.getArgs().get(0);
            String value = String.join(" ", context.getArgs().subList(1, context.getArgs().size()));
            return setConfig(context, key, value);
        }
    }

    private CommandResult showAllConfig(CommandContext context) {
        GlobalConfig config = configManager.getGlobalConfig();

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(config);
            context.getOutput().println("\n当前配置:\n" + json);
            return CommandResult.success("");
        } catch (Exception e) {
            return CommandResult.failure("显示配置失败: " + e.getMessage());
        }
    }

    private CommandResult showConfig(CommandContext context, String key) {
        GlobalConfig config = configManager.getGlobalConfig();

        String value = switch (key.toLowerCase()) {
            case "safemode" -> String.valueOf(config.getSafeMode());
            case "verbose" -> String.valueOf(config.isVerbose());
            default -> "未知配置项: " + key;
        };

        context.getOutput().println(key + " = " + value);
        return CommandResult.success("");
    }

    private CommandResult setConfig(CommandContext context, String key, String value) {
        GlobalConfig config = configManager.getGlobalConfig();

        switch (key.toLowerCase()) {
            case "safemode" -> config.setSafeMode(Boolean.parseBoolean(value));
            case "verbose" -> config.setVerbose(Boolean.parseBoolean(value));
            default -> {
                return CommandResult.failure("未知配置项: " + key);
            }
        }

        // 保存配置
        configManager.saveGlobalConfig();

        context.getOutput().success("配置已更新: " + key + " = " + value);
        return CommandResult.success("");
    }
}
