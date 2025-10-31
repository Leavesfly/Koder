package io.leavesfly.koder.agent.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM提供商自动配置
 * 根据配置文件自动注册LLM提供商
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LLMProviderConfig.class)
@RequiredArgsConstructor
public class LLMProviderAutoConfiguration {

    private final LLMProviderConfig config;

    /**
     * 创建LLM提供商注册中心
     */
    @Bean
    public LLMProviderRegistry llmProviderRegistry() {
        LLMProviderRegistry registry = new LLMProviderRegistry();

        // 注册DeepSeek
        if (config.getDeepseek().isEnabled() && config.getDeepseek().getApiKey() != null) {
            DeepSeekProvider deepSeek = new DeepSeekProvider(
                config.getDeepseek().getApiKey(),
                config.getDeepseek().getBaseUrl()
            );
            registry.registerProvider(deepSeek);
            log.info("DeepSeek提供商已启用");
        }

        // 注册通义千问
        if (config.getQwen().isEnabled() && config.getQwen().getApiKey() != null) {
            QwenProvider qwen = new QwenProvider(
                config.getQwen().getApiKey(),
                config.getQwen().getBaseUrl()
            );
            registry.registerProvider(qwen);
            log.info("Qwen提供商已启用");
        }

        // 注册Ollama
        if (config.getOllama().isEnabled()) {
            OllamaProvider ollama = new OllamaProvider(
                config.getOllama().getBaseUrl()
            );
            registry.registerProvider(ollama);
            log.info("Ollama提供商已启用");
        }

        log.info("LLM提供商注册完成，总计: {} 个", registry.getProviderCount());
        return registry;
    }
}
