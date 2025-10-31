package io.leavesfly.koder.core.config;

/**
 * 提供商类型枚举
 * 支持多种AI模型提供商
 */
public enum ProviderType {
    DEMO("demo"),          // 演示模式（用于测试）
    ANTHROPIC("anthropic"),
    OPENAI("openai"),
    MISTRAL("mistral"),
    DEEPSEEK("deepseek"),
    KIMI("kimi"),
    QWEN("qwen"),
    GLM("glm"),
    MINIMAX("minimax"),
    BAIDU_QIANFAN("baidu-qianfan"),
    SILICONFLOW("siliconflow"),
    BIGDREAM("bigdream"),
    OPENDEV("opendev"),
    XAI("xai"),
    GROQ("groq"),
    GEMINI("gemini"),
    OLLAMA("ollama"),
    AZURE("azure"),
    CUSTOM("custom"),
    CUSTOM_OPENAI("custom-openai");

    private final String value;

    ProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProviderType fromValue(String value) {
        for (ProviderType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown provider type: " + value);
    }
}
