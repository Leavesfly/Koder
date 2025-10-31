package io.leavesfly.koder.cli;

import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandRegistry;
import io.leavesfly.koder.cli.repl.REPLEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Koder CLI主应用
 * Spring Boot启动类
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {
        "io.leavesfly.koder.core",
        "io.leavesfly.koder.model",
        "io.leavesfly.koder.tool",
        "io.leavesfly.koder.mcp",
        "io.leavesfly.koder.agent",
        "io.leavesfly.koder.cli"
})
@RequiredArgsConstructor
@Order(2) // 在CLIConfigInitializer之后运行
public class KoderCliApplication implements CommandLineRunner {

    private final REPLEngine replEngine;
    private final CommandRegistry commandRegistry;
    private final List<Command> commands;

    public static void main(String[] args) {
        // 关闭Spring Boot的横幅
        SpringApplication app = new SpringApplication(KoderCliApplication.class);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        log.info("Koder CLI启动中...");

        // 注册所有命令
        commandRegistry.registerAll(commands);

        // 启动REPL
        replEngine.start();
    }
}
