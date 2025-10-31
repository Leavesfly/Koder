package io.leavesfly.koder.agent.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM配置属性
 * 从配置文件加载LLM提供商配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "koder.llm")
public class LLMProviderConfig {

    /**
     * DeepSeek配置
     */
    private ProviderSettings deepseek = new ProviderSettings();

    /**
     * 通义千问配置
     */
    private ProviderSettings qwen = new ProviderSettings();

    /**
     * Ollama配置
     */
    private ProviderSettings ollama = new ProviderSettings();

    /**
     * 提供商配置
     */
    @Data
    public static class ProviderSettings {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * API Key
         */
        private String apiKey;

        /**
         * Base URL
         */
        private String baseUrl;

        /**
         * 默认模型名称
         */
        private String defaultModel;

        /**
         * 超时时间(秒)
         */
        private int timeout = 60;

        /**
         * 额外参数
         */
        private Map<String, Object> params = new HashMap<>();
    }
}
