package io.leavesfly.koder.core.ai;

import io.leavesfly.koder.core.ai.impl.DeepSeekLlmService;
import io.leavesfly.koder.core.ai.impl.OllamaLlmService;
import io.leavesfly.koder.core.ai.impl.QwenLlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM服务集成测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "koder.ai.enabled=true",
        "koder.ai.default-provider=deepseek",
        "koder.ai.deepseek.enabled=true",
        "koder.ai.qwen.enabled=true",
        "koder.ai.ollama.enabled=false"
})
class LlmIntegrationTest {

    @Autowired
    private LlmManager llmManager;

    @Autowired
    private DeepSeekLlmService deepSeekLlmService;

    @Autowired
    private QwenLlmService qwenLlmService;

    @Autowired
    private OllamaLlmService ollamaLlmService;

    @BeforeEach
    void setUp() {
        assertNotNull(llmManager);
    }

    @Test
    void testLlmManagerInitialization() {
        assertNotNull(llmManager);
        assertNotNull(deepSeekLlmService);
        assertNotNull(qwenLlmService);
        assertNotNull(ollamaLlmService);

        assertEquals(LlmProvider.DEEPSEEK, deepSeekLlmService.getProvider());
        assertEquals(LlmProvider.QWEN, qwenLlmService.getProvider());
        assertEquals(LlmProvider.OLLAMA, ollamaLlmService.getProvider());
    }

    @Test
    void testGetDefaultService() {
        LlmService defaultService = llmManager.getDefaultService();
        assertNotNull(defaultService);
        assertEquals(LlmProvider.DEEPSEEK, defaultService.getProvider());
    }

    @Test
    void testGetServiceByProvider() {
        LlmService deepSeekService = llmManager.getService(LlmProvider.DEEPSEEK);
        assertNotNull(deepSeekService);
        assertEquals(LlmProvider.DEEPSEEK, deepSeekService.getProvider());

        LlmService qwenService = llmManager.getService(LlmProvider.QWEN);
        assertNotNull(qwenService);
        assertEquals(LlmProvider.QWEN, qwenService.getProvider());
    }

    @Test
    void testGetServiceByCode() {
        LlmService service = llmManager.getService("deepseek");
        assertNotNull(service);
        assertEquals(LlmProvider.DEEPSEEK, service.getProvider());
    }

    @Test
    void testGetServiceStatus() {
        var status = llmManager.getServiceStatus();
        assertNotNull(status);
        assertTrue((Boolean) status.get("enabled"));
        assertEquals("deepseek", status.get("defaultProvider"));
        assertNotNull(status.get("providers"));
    }

    @Test
    void testSimpleChatRequest() {
        // 注意：此测试需要配置有效的API密钥才能通过
        // 如果没有API密钥，服务将不可用，跳过实际调用

        LlmChatRequest request = LlmChatRequest.builder()
                .message("Hello")
                .build();

        assertNotNull(request);
        assertEquals("Hello", request.getMessage());
        assertFalse(request.isStream());
    }

    @Test
    void testChatRequestWithOptions() {
        LlmChatRequest request = LlmChatRequest.builder()
                .message("Explain dependency injection")
                .systemPrompt("You are a Java expert")
                .temperature(0.5)
                .maxTokens(2048)
                .stream(true)
                .build();

        assertNotNull(request);
        assertEquals("Explain dependency injection", request.getMessage());
        assertEquals("You are a Java expert", request.getSystemPrompt());
        assertEquals(0.5, request.getTemperature());
        assertEquals(2048, request.getMaxTokens());
        assertTrue(request.isStream());
    }

    @Test
    void testHealthCheck() {
        // 注意：此测试需要配置有效的API密钥才能通过
        // 如果没有API密钥，健康检查将返回false

        StepVerifier.create(llmManager.healthCheckAll())
                .assertNext(healthStatus -> {
                    assertNotNull(healthStatus);
                    assertFalse(healthStatus.isEmpty());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    @Test
    void testProviderAvailability() {
        // DeepSeek和Qwen的可用性取决于是否配置了API密钥
        // Ollama默认禁用
        assertFalse(llmManager.isServiceAvailable(LlmProvider.OLLAMA));
    }
}
