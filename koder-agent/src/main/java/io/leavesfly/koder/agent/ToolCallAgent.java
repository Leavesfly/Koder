package io.leavesfly.koder.agent;

import io.leavesfly.koder.agent.executor.AgentExecutor;
import io.leavesfly.koder.tool.Tool;
import io.leavesfly.koder.tool.ToolResponse;
import io.leavesfly.koder.tool.ToolUseContext;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent执行类
 * 封装Agent的执行逻辑,包括工具调用和历史管理
 */
@RequiredArgsConstructor
@Slf4j
public class ToolCallAgent {

    private final AgentConfig config;
    private final List<Tool<?, ?>> allowedTools;
    private final AgentExecutor.ConversationHistory history;
    private final ToolExecutor toolExecutor;

    public Flux<String> execute(String userInput, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                // 添加用户消息到历史
                history.addUserMessage(userInput);

                // 添加系统提示词(如果是第一条消息)
                if (history.getMessageCount() == 1 && config.getSystemPrompt() != null) {
                    history.addSystemMessage(config.getSystemPrompt());
                }

                //  TODO 临时示例实现
                String response = generateMockResponse(userInput, context);


                history.addAssistantMessage(response);
                sink.next(response);
                sink.complete();

                log.info("Agent执行完成: {}", config.getAgentType());

            } catch (Exception e) {
                log.error("Agent执行异常", e);
                sink.error(new AgentExecutor.AgentExecutionException("Agent执行异常: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 生成模拟响应(临时实现)
     *
     */
    private String generateMockResponse(String userInput, ToolUseContext context) {
        return String.format("[Agent %s] 收到输入: %s\n可用工具: %s\n历史消息数: %d",
                config.getAgentType(),
                userInput,
                allowedTools.stream().map(Tool::getName).collect(Collectors.joining(", ")),
                history.getMessageCount());
    }

    /**
     * 执行工具调用
     * 将工具调用请求转发给ToolExecutor
     */
    @SuppressWarnings("unchecked")
    public <I, O> Mono<String> executeTool(String toolName, I input, ToolUseContext context) {
        // 检查工具权限
        boolean allowed = allowedTools.stream()
                .anyMatch(tool -> tool.getName().equals(toolName));

        if (!allowed) {
            return Mono.error(new AgentExecutor.AgentExecutionException(
                    String.format("Agent %s 不允许使用工具: %s",
                            config.getAgentType(), toolName)));
        }

        // 执行工具并获取结果
        return toolExecutor.execute(toolName, input, context)
                .filter(response -> response.getType() == ToolResponse.ResponseType.RESULT)
                .map(response -> {
                    Tool<I, O> tool = (Tool<I, O>) toolExecutor.getTool(toolName);
                    if (tool != null) {
                        Object result = tool.renderResultForAssistant((O) response.getData());
                        return result != null ? result.toString() : "";
                    }
                    return response.getData() != null ? response.getData().toString() : "";
                })
                .next()
                .doOnError(error -> log.error("工具执行失败: {}", toolName, error));
    }

    /**
     * 获取Agent配置
     */
    public AgentConfig getConfig() {
        return config;
    }

    /**
     * 获取允许的工具列表
     */
    public List<Tool<?, ?>> getAllowedTools() {
        return Collections.unmodifiableList(allowedTools);
    }

    /**
     * 获取工具Schema列表(用于Function Calling)
     */
    public List<Map<String, Object>> getToolSchemas() {
        return allowedTools.stream()
                .map(tool -> {
                    Map<String, Object> schema = new HashMap<>();
                    schema.put("name", tool.getName());
                    schema.put("description", tool.getDescription());
                    schema.put("parameters", tool.getInputSchema());
                    return schema;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取会话历史
     */
    public AgentExecutor.ConversationHistory getHistory() {
        return history;
    }
}
