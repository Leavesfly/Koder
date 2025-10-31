package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.core.config.GlobalConfig;
import io.leavesfly.koder.core.config.ModelProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模型命令
 * 查看和切换AI模型
 */
@Component
@RequiredArgsConstructor
public class ModelCommand implements Command {

    private final ConfigManager configManager;

    @Override
    public String getName() {
        return "model";
    }

    @Override
    public String getDescription() {
        return "查看或切换AI模型";
    }

    @Override
    public String getUsage() {
        return "/model [模型名称]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.getArgs().isEmpty()) {
            // 显示当前模型
            return showCurrentModel(context);
        } else {
            // 切换模型
            String modelName = context.getArgs().get(0);
            return switchModel(context, modelName);
        }
    }

    private CommandResult showCurrentModel(CommandContext context) {
        GlobalConfig config = configManager.getGlobalConfig();
        String mainModelName = config.getModelPointers().getMain();
        
        // 查找主模型
        ModelProfile mainModel = config.getModelProfiles().stream()
                .filter(p -> p.getModelName().equals(mainModelName))
                .findFirst()
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("\n当前模型配置:\n");
        sb.append("  主模型: ").append(mainModel != null ? mainModel.getModelName() : "未配置").append("\n");

        if (config.getModelProfiles() != null && !config.getModelProfiles().isEmpty()) {
            sb.append("\n可用模型:\n");
            config.getModelProfiles().forEach(profile -> {
                sb.append("  - ").append(profile.getModelName())
                  .append(" (").append(profile.getProvider()).append(")\n");
            });
        }

        context.getOutput().println(sb.toString());
        return CommandResult.success("");
    }

    private CommandResult switchModel(CommandContext context, String modelName) {
        GlobalConfig config = configManager.getGlobalConfig();

        // 查找模型
        boolean modelExists = config.getModelProfiles().stream()
                .anyMatch(p -> p.getModelName().equals(modelName));
        
        if (!modelExists) {
            return CommandResult.failure("模型 '" + modelName + "' 不存在");
        }

        // 更新主模型指针
        config.getModelPointers().setMain(modelName);

        // 保存配置
        configManager.saveGlobalConfig();

        context.getOutput().success("已切换到模型: " + modelName);
        return CommandResult.success("");
    }
}
