package io.leavesfly.koder.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 通义千问 LLM提供商实现
 */
@Slf4j
public class QwenProvider implements LLMProvider {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String baseUrl;

    public QwenProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://dashscope.aliyuncs.com/compatible-mode";
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    @Override
    public Mono<LLMResponse> call(List<Map<String, Object>> messages, 
                                  List<Map<String, Object>> tools, 
                                  String modelName) {
        WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName != null ? modelName : "qwen-max");
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("tools", tools);

        log.debug("Qwen请求: model={}", modelName);

        return client.post()
            .uri("/v1/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseResponse)
            .doOnError(e -> log.error("Qwen API调用失败", e));
    }

    @Override
    public boolean supports(String modelName) {
        if (modelName == null) return false;
        return modelName.toLowerCase().contains("qwen");
    }

    private LLMResponse parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
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
            }
            
            return new LLMResponse("", Collections.emptyList());
        } catch (Exception e) {
            log.error("解析Qwen响应失败", e);
            return new LLMResponse("Error: " + e.getMessage(), Collections.emptyList());
        }
    }
}
