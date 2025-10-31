package io.leavesfly.koder.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Ollama LLM提供商实现
 */
@Slf4j
public class OllamaProvider implements LLMProvider {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    public OllamaProvider(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:11434";
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public Mono<LLMResponse> call(List<Map<String, Object>> messages, 
                                  List<Map<String, Object>> tools, 
                                  String modelName) {
        WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName != null ? modelName : "llama2");
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("tools", tools);

        log.debug("Ollama请求: model={}", modelName);

        return client.post()
            .uri("/api/chat")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseResponse)
            .doOnError(e -> log.error("Ollama API调用失败", e));
    }

    @Override
    public boolean supports(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.contains("llama") || lower.contains("mistral") || 
               lower.contains("codellama") || lower.contains("ollama");
    }

    private LLMResponse parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode message = jsonNode.get("message");
            
            if (message != null) {
                String content = "";
                JsonNode contentNode = message.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    content = contentNode.asText();
                }
                
                JsonNode toolCallsNode = message.get("tool_calls");
                if (toolCallsNode != null && toolCallsNode.isArray() && toolCallsNode.size() > 0) {
                    List<Map<String, Object>> toolCalls = new ArrayList<>();
                    for (JsonNode toolCall : toolCallsNode) {
                        Map<String, Object> call = objectMapper.convertValue(toolCall, Map.class);
                        toolCalls.add(call);
                    }
                    return new LLMResponse(content, toolCalls);
                }
                
                return new LLMResponse(content, Collections.emptyList());
            }
            
            return new LLMResponse("", Collections.emptyList());
        } catch (Exception e) {
            log.error("解析Ollama响应失败", e);
            return new LLMResponse("Error: " + e.getMessage(), Collections.emptyList());
        }
    }
}
