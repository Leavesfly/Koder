package io.leavesfly.koder.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.koder.agent.executor.AgentExecutor;
import io.leavesfly.koder.agent.llm.LLMProvider;
import io.leavesfly.koder.agent.llm.LLMProviderRegistry;
import io.leavesfly.koder.tool.Tool;
import io.leavesfly.koder.tool.ToolResponse;
import io.leavesfly.koder.tool.ToolUseContext;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent执行类
 * 封装Agent的执行逻辑,包括工具调用和历史管理
 * 
 * 重构后的设计:
 * - 解耦具体LLM实现，通过LLMProviderRegistry调用
 * - 支持通过配置文件动态加载LLM提供商
 * - 简化核心逻辑，专注于工具调用循环
 */
@RequiredArgsConstructor
@Slf4j
public class ToolCallAgent {

    private final AgentConfig config;
    private final List<Tool<?, ?>> allowedTools;
    private final AgentExecutor.ConversationHistory history;
    private final ToolExecutor toolExecutor;
    private final LLMProviderRegistry llmProviderRegistry;

    private static final int MAX_ITERATIONS = 20;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行Agent任务
     * @param userInput 用户输入
     * @param context 执行上下文
     * @return 流式响应
     */
    public Flux<String> execute(String userInput, ToolUseContext context) {
        return Flux.defer(() -> {
            try {
                // 添加用户消息到历史
                history.addUserMessage(userInput);

                // 构建初始请求消息
                List<Map<String, Object>> messages = buildMessages();

                // 循环调用LLM直到不再需要工具调用
                return executeWithToolLoop(messages, context, 0);
            } catch (Exception e) {
                log.error("Agent执行失败", e);
                return Flux.error(e);
            }
        });
    }

    /**
     * 循环执行LLM调用和工具调用
     */
    private Flux<String> executeWithToolLoop(List<Map<String, Object>> messages, 
                                              ToolUseContext context, 
                                              int iteration) {
        if (iteration >= MAX_ITERATIONS) {
            log.warn("达到最大循环次数限制: {}", MAX_ITERATIONS);
            return Flux.just("\n[警告: 已达到最大工具调用次数限制]");
        }

        // 调用LLM并收集完整响应
        return callLLM(messages)
            .flatMapMany(llmResponse -> {
                try {
                    // 检查是否包含工具调用
                    if (llmResponse.hasToolCalls()) {
                        log.info("检测到工具调用，数量: {}", llmResponse.getToolCalls().size());
                        
                        // 执行工具调用
                        return executeToolCalls(llmResponse.getToolCalls(), context)
                            .collectList()
                            .flatMapMany(toolResults -> {
                                // 将助手消息添加到历史
                                String assistantContent = llmResponse.getContent() != null ? 
                                    llmResponse.getContent() : "";
                                history.addAssistantMessage(assistantContent);
                                
                                // 构建新的消息列表
                                List<Map<String, Object>> newMessages = new ArrayList<>(messages);
                                
                                // 添加助手的响应
                                Map<String, Object> assistantMsg = new HashMap<>();
                                assistantMsg.put("role", "assistant");
                                assistantMsg.put("content", assistantContent);
                                assistantMsg.put("tool_calls", llmResponse.getToolCalls());
                                newMessages.add(assistantMsg);
                                
                                // 添加工具执行结果
                                for (ToolExecutionResult result : toolResults) {
                                    Map<String, Object> toolMsg = new HashMap<>();
                                    toolMsg.put("role", "tool");
                                    toolMsg.put("tool_call_id", result.getToolCallId());
                                    toolMsg.put("content", result.getResult());
                                    newMessages.add(toolMsg);
                                    
                                    log.info("工具 {} 执行完成", result.getToolName());
                                }
                                
                                // 递归调用
                                return executeWithToolLoop(newMessages, context, iteration + 1);
                            });
                    } else {
                        // 没有工具调用,返回最终响应
                        String content = llmResponse.getContent();
                        history.addAssistantMessage(content);
                        return Flux.just(content);
                    }
                } catch (Exception e) {
                    log.error("处理LLM响应失败", e);
                    return Flux.just(llmResponse.getContent());
                }
            });
    }

    /**
     * 调用LLM API
     */
    private Mono<LLMProvider.LLMResponse> callLLM(List<Map<String, Object>> messages) {
        String modelName = config.getModelName();
        List<Map<String, Object>> tools = buildToolSchemas();

        log.debug("调用LLM: model={}, tools={}", modelName, tools.size());

        return llmProviderRegistry.call(modelName, messages, tools);
    }

    /**
     * 构建消息列表
     */
    private List<Map<String, Object>> buildMessages() {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 添加系统提示词
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isEmpty()) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", config.getSystemPrompt());
            messages.add(systemMsg);
        }

        // 添加历史消息
        for (AgentExecutor.ChatMessage msg : history.getMessages()) {
            Map<String, Object> message = new HashMap<>();
            message.put("role", msg.getRole());
            message.put("content", msg.getContent());
            messages.add(message);
        }

        return messages;
    }

    /**
     * 构建工具Schema列表
     */
    private List<Map<String, Object>> buildToolSchemas() {
        return allowedTools.stream()
            .map(tool -> {
                Map<String, Object> schema = new HashMap<>();
                schema.put("type", "function");
                
                Map<String, Object> function = new HashMap<>();
                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                function.put("parameters", tool.getInputSchema());
                
                schema.put("function", function);
                return schema;
            })
            .collect(Collectors.toList());
    }

    /**
     * 执行工具调用
     */
    @SuppressWarnings("unchecked")
    private Flux<ToolExecutionResult> executeToolCalls(List<Map<String, Object>> toolCalls, 
                                                       ToolUseContext context) {
        return Flux.fromIterable(toolCalls)
            .flatMap(toolCall -> {
                try {
                    String toolCallId = (String) toolCall.get("id");
                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                    String toolName = (String) function.get("name");
                    Map<String, Object> arguments = objectMapper.readValue(
                        (String) function.get("arguments"), 
                        Map.class
                    );

                    log.info("执行工具: {} (id={})", toolName, toolCallId);

                    return toolExecutor.execute(toolName, arguments, context)
                        .filter(resp -> resp.getType() == ToolResponse.ResponseType.RESULT)
                        .map(resp -> {
                            try {
                                return new ToolExecutionResult(
                                    toolCallId,
                                    toolName,
                                    objectMapper.writeValueAsString(resp.getData())
                                );
                            } catch (Exception e) {
                                return new ToolExecutionResult(
                                    toolCallId,
                                    toolName,
                                    String.valueOf(resp.getData())
                                );
                            }
                        })
                        .onErrorResume(e -> {
                            log.error("工具执行失败: {}", toolName, e);
                            return Mono.just(new ToolExecutionResult(
                                toolCallId,
                                toolName,
                                "Error: " + e.getMessage()
                            ));
                        });
                } catch (Exception e) {
                    log.error("解析工具调用参数失败", e);
                    return Mono.empty();
                }
            });
    }

    /**
     * 工具执行结果类
     */
    private static class ToolExecutionResult {
        private final String toolCallId;
        private final String toolName;
        private final String result;

        public ToolExecutionResult(String toolCallId, String toolName, String result) {
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.result = result;
        }

        public String getToolCallId() {
            return toolCallId;
        }

        public String getToolName() {
            return toolName;
        }

        public String getResult() {
            return result;
        }
    }
}
