package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * doctor命令 - 检查Koder安装的健康状况
 */
@Component
@RequiredArgsConstructor
public class DoctorCommand implements Command {

    @Override
    public String getName() {
        return "doctor";
    }

    @Override
    public String getDescription() {
        return "检查Koder安装的健康状况";
    }

    @Override
    public String getUsage() {
        return "/doctor";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        StringBuilder output = new StringBuilder();
        output.append("\n=== Koder 健康检查 ===\n\n");

        // Java版本检查
        String javaVersion = System.getProperty("java.version");
        output.append("✓ Java版本: ").append(javaVersion);
        if (javaVersion.startsWith("17") || javaVersion.startsWith("18") || javaVersion.startsWith("19") ||
                javaVersion.startsWith("20") || javaVersion.startsWith("21")) {
            output.append(" (正常)\n");
        } else {
            output.append(" (警告: 推荐Java 17+)\n");
        }

        // 操作系统
        String os = System.getProperty("os.name");
        output.append("✓ 操作系统: ").append(os).append("\n");

        // 工作目录
        String cwd = System.getProperty("user.dir");
        output.append("✓ 工作目录: ").append(cwd).append("\n");

        // 用户主目录
        String home = System.getProperty("user.home");
        output.append("✓ 用户主目录: ").append(home).append("\n");

        // 配置文件检查
        java.io.File globalConfig = new java.io.File(home, ".koder.json");
        if (globalConfig.exists()) {
            output.append("✓ 全局配置: 存在\n");
        } else {
            output.append("⚠ 全局配置: 不存在\n");
        }

        java.io.File projectConfig = new java.io.File(cwd, ".koder.json");
        if (projectConfig.exists()) {
            output.append("✓ 项目配置: 存在\n");
        } else {
            output.append("⚠ 项目配置: 不存在\n");
        }

        // 内存
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        output.append("\n内存信息:\n");
        output.append("  最大内存: ").append(maxMemory).append(" MB\n");
        output.append("  已分配: ").append(totalMemory).append(" MB\n");
        output.append("  可用: ").append(freeMemory).append(" MB\n");

        output.append("\n✓ Koder安装正常\n");

        return CommandResult.success(output.toString());
    }
}
