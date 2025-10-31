package io.leavesfly.koder.agent.examples;

import io.leavesfly.koder.agent.llm.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * LLM Provider重构架构演示
 * 
 * 展示重构后的架构优势:
 * 1. LLM提供商解耦 - 通过接口抽象实现
 * 2. 配置文件加载 - 支持通过application.yml配置
 * 3. 动态注册机制 - 运行时可动态添加提供商
 * 4. 统一调用接口 - 通过注册中心统一路由
 */
@Slf4j
public class LLMProviderArchitectureDemo {

    /**
     * 演示1: 手动创建和注册提供商
     */
    public static void demonstrateManualRegistration() {
        log.info("\n===== 演示1: 手动创建和注册提供商 =====");

        // 创建注册中心
        LLMProviderRegistry registry = new LLMProviderRegistry();

        // 创建DeepSeek提供商
        DeepSeekProvider deepSeek = new DeepSeekProvider(
            "your-deepseek-api-key",
            "https://api.deepseek.com"
        );
        registry.registerProvider(deepSeek);

        // 创建Qwen提供商
        QwenProvider qwen = new QwenProvider(
            "your-qwen-api-key",
            "https://dashscope.aliyuncs.com/compatible-mode"
        );
        registry.registerProvider(qwen);

        // 创建Ollama提供商
        OllamaProvider ollama = new OllamaProvider("http://localhost:11434");
        registry.registerProvider(ollama);

        log.info("已注册提供商数量: {}", registry.getProviderCount());
        
        // 测试模型路由
        testModelRouting(registry);
    }

    /**
     * 演示2: 通过配置文件自动加载
     */
    public static void demonstrateAutoConfiguration() {
        log.info("\n===== 演示2: 通过配置文件自动加载 =====");
        
        log.info("配置文件示例 (application.yml):");
        log.info("""
            koder:
              llm:
                deepseek:
                  enabled: true
                  api-key: ${DEEPSEEK_API_KEY}
                  base-url: https://api.deepseek.com
                  default-model: deepseek-chat
                  
                qwen:
                  enabled: true
                  api-key: ${QWEN_API_KEY}
                  base-url: https://dashscope.aliyuncs.com/compatible-mode
                  default-model: qwen-max
                  
                ollama:
                  enabled: true
                  base-url: http://localhost:11434
                  default-model: llama2
            """);
        
        log.info("Spring Boot会自动加载LLMProviderAutoConfiguration");
        log.info("根据配置创建相应的Provider实例并注册到Registry");
    }

    /**
     * 演示3: 扩展自定义提供商
     */
    public static void demonstrateCustomProvider() {
        log.info("\n===== 演示3: 扩展自定义提供商 =====");
        
        log.info("创建自定义提供商只需实现LLMProvider接口:");
        log.info("""
            public class CustomProvider implements LLMProvider {
                @Override
                public String getProviderName() {
                    return "custom";
                }
                
                @Override
                public Mono<LLMResponse> call(List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools,
                                              String modelName) {
                    // 自定义实现
                }
                
                @Override
                public boolean supports(String modelName) {
                    return modelName.contains("custom");
                }
            }
            """);
    }

    /**
     * 演示4: 统一调用接口
     */
    public static void demonstrateUnifiedInterface() {
        log.info("\n===== 演示4: 统一调用接口 =====");
        
        LLMProviderRegistry registry = new LLMProviderRegistry();
        
        // 注册提供商
        registry.registerProvider(new DeepSeekProvider("key", null));
        registry.registerProvider(new QwenProvider("key", null));
        registry.registerProvider(new OllamaProvider(null));
        
        // 统一调用方式
        List<Map<String, Object>> messages = buildSampleMessages();
        List<Map<String, Object>> tools = buildSampleTools();
        
        // 自动路由到正确的提供商
        log.info("调用deepseek-chat模型:");
        registry.findProvider("deepseek-chat")
            .ifPresent(p -> log.info("  -> 路由到提供商: {}", p.getProviderName()));
        
        log.info("调用qwen-max模型:");
        registry.findProvider("qwen-max")
            .ifPresent(p -> log.info("  -> 路由到提供商: {}", p.getProviderName()));
        
        log.info("调用llama2模型:");
        registry.findProvider("llama2")
            .ifPresent(p -> log.info("  -> 路由到提供商: {}", p.getProviderName()));
    }

    /**
     * 测试模型路由
     */
    private static void testModelRouting(LLMProviderRegistry registry) {
        log.info("\n测试模型路由:");
        
        String[] modelNames = {
            "deepseek-chat",
            "deepseek-coder",
            "qwen-max",
            "qwen-plus",
            "llama2",
            "mistral"
        };
        
        for (String modelName : modelNames) {
            registry.findProvider(modelName).ifPresentOrElse(
                provider -> log.info("  {} -> {}", modelName, provider.getProviderName()),
                () -> log.warn("  {} -> 未找到提供商", modelName)
            );
        }
    }

    /**
     * 构建示例消息
     */
    private static List<Map<String, Object>> buildSampleMessages() {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个有帮助的AI助手");
        messages.add(systemMsg);
        
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "请帮我读取文件列表");
        messages.add(userMsg);
        
        return messages;
    }

    /**
     * 构建示例工具
     */
    private static List<Map<String, Object>> buildSampleTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "list_files");
        function.put("description", "列出目录下的文件");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.of(
            "path", Map.of("type", "string", "description", "目录路径")
        ));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        tools.add(tool);
        return tools;
    }

    /**
     * 架构优势总结
     */
    public static void printArchitectureBenefits() {
        log.info("\n===== 重构架构优势总结 =====");
        log.info("✅ 1. 解耦设计 - LLM具体实现与Agent逻辑分离");
        log.info("✅ 2. 易于扩展 - 新增Provider只需实现接口");
        log.info("✅ 3. 配置驱动 - 通过配置文件灵活控制");
        log.info("✅ 4. 动态注册 - 运行时可添加/移除提供商");
        log.info("✅ 5. 统一接口 - 所有LLM使用相同调用方式");
        log.info("✅ 6. 测试友好 - 易于Mock和单元测试");
    }

    public static void main(String[] args) {
        demonstrateManualRegistration();
        demonstrateAutoConfiguration();
        demonstrateCustomProvider();
        demonstrateUnifiedInterface();
        printArchitectureBenefits();
    }
}
