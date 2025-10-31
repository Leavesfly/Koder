package io.leavesfly.koder.cli.config;

import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.core.config.GlobalConfig;
import io.leavesfly.koder.core.config.ModelPointers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * CLI配置初始化器
 * 在启动时检查并初始化必要的配置
 */
@Slf4j
@Component
@Order(1) // 优先级最高
@RequiredArgsConstructor
public class CLIConfigInitializer implements CommandLineRunner {

    private final ConfigManager configManager;

    @Override
    public void run(String... args) {
        log.info("初始化CLI配置...");

        GlobalConfig config = configManager.getGlobalConfig();


        // 确保有模型指针配置
        if (config.getModelPointers() == null) {

            ModelPointers modelPointers = new ModelPointers();
            //todo
            modelPointers.setMain("xxx");

            config.setModelPointers(modelPointers);
        }


        // 保存配置
        configManager.saveGlobalConfig();

        log.info("CLI配置初始化完成");
    }

}
