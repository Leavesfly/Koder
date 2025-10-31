package io.leavesfly.koder.cli.config;

import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.core.config.GlobalConfig;
import io.leavesfly.koder.core.config.ModelPointers;
import io.leavesfly.koder.core.config.ModelProfile;
import io.leavesfly.koder.core.config.ProviderType;
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

        // 确保有模型配置列表
        if (config.getModelProfiles() == null) {
            config.setModelProfiles(new ArrayList<>());
        }

        // 如果没有任何模型配置，创建默认的演示模型
        if (config.getModelProfiles().isEmpty()) {
            log.info("未找到模型配置，创建默认演示模型...");
            createDemoModel(config);
        }

        // 确保有模型指针配置
        if (config.getModelPointers() == null) {
            config.setModelPointers(new ModelPointers());
        }

        // 如果主模型指针未设置，设置为第一个可用模型
        if (config.getModelPointers().getMain() == null && !config.getModelProfiles().isEmpty()) {
            String firstModel = config.getModelProfiles().get(0).getModelName();
            config.getModelPointers().setMain(firstModel);
            log.info("设置主模型指针: {}", firstModel);
        }

        // 保存配置
        configManager.saveGlobalConfig();

        log.info("CLI配置初始化完成");
    }

    /**
     * 创建演示模型配置
     */
    private void createDemoModel(GlobalConfig config) {
        ModelProfile demoProfile = ModelProfile.builder()
                .provider(ProviderType.DEMO)
                .modelName("demo-model")
                .apiKey("demo")
                .createdAt(System.currentTimeMillis())
                .build();

        config.getModelProfiles().add(demoProfile);

        log.info("已创建演示模型配置: demo");
    }
}
