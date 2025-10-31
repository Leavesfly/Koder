package io.leavesfly.koder.agent.config;

import io.leavesfly.koder.agent.AgentRegistry;
import io.leavesfly.koder.agent.watcher.AgentFileWatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 代理系统初始化器
 * 启动时加载代理配置并启动文件监听
 */
@Slf4j
@Component
@Order(5) // 在其他模块之后初始化
@RequiredArgsConstructor
public class AgentInitializer implements CommandLineRunner {

    private final AgentRegistry agentRegistry;
    private final AgentFileWatcher agentFileWatcher;

    @Override
    public void run(String... args) {
        log.info("初始化代理系统...");

        // 初始化代理注册表
        agentRegistry.initialize();

        // 启动文件监听器（热重载）
        try {
            agentFileWatcher.start();
            log.info("代理文件热重载已启用");
        } catch (Exception e) {
            log.warn("启动代理文件监听器失败，热重载不可用", e);
        }

        log.info("代理系统初始化完成");
    }
}
