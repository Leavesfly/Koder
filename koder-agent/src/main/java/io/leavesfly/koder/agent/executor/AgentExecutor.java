package io.leavesfly.koder.agent.executor;

import io.leavesfly.koder.agent.AgentConfig;
import io.leavesfly.koder.agent.AgentRegistry;
import io.leavesfly.koder.agent.ToolCallAgent;
import io.leavesfly.koder.core.llm.LLMProviderRegistry;
import io.leavesfly.koder.tool.Tool;
import io.leavesfly.koder.tool.ToolUseContext;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 代理执行器
 * 基于Spring AI ChatClient实现Agent执行逻辑
 * <p>
 * 核心功能:
 * 1. 集成koder工具集 - 通过ToolExecutor管理和调用工具
 * 2. 会话历史管理 - 维护多会话的对话历史
 * 3. AgentConfig转Agent - 从配置创建可执行的Agent实例
 * 4. 基于Spring ChatClient - 使用Spring AI框架执行对话
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutor {

    private final AgentRegistry agentRegistry;
    private final ToolExecutor toolExecutor;
    private final LLMProviderRegistry llmProviderRegistry;

    // 会话历史管理(sessionId -> 会话历史)
    private final Map<String, ConversationHistory> sessionHistories = new ConcurrentHashMap<>();

    /**
     * 执行Agent任务(流式)
     *
     * @param agentType Agent类型
     * @param userInput 用户输入
     * @param context   执行上下文
     * @return 流式响应
     */
    public Flux<String> executeAgent(String agentType, String userInput, ToolUseContext context) {
        return Flux.defer(() -> {
            // 获取Agent配置
            AgentConfig agentConfig = agentRegistry.getAgentByType(agentType)
                    .orElseThrow(() -> new AgentExecutionException("未找到Agent: " + agentType));

            // 构建Agent实例
            ToolCallAgent agent = buildAgent(agentConfig, context);

            // 执行Agent
            return agent.execute(userInput, context);
        });
    }

    /**
     * 执行Agent任务(同步)
     *
     * @param agentType Agent类型
     * @param userInput 用户输入
     * @param context   执行上下文
     * @return 最终结果
     */
    public String executeAgentSync(String agentType, String userInput, ToolUseContext context) {
        return executeAgent(agentType, userInput, context)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
    }

    /**
     * 构建Agent实例
     *
     * @param config  Agent配置
     * @param context 执行上下文
     * @return Agent实例
     */
    private ToolCallAgent buildAgent(AgentConfig config, ToolUseContext context) {
        // 过滤Agent允许使用的工具
        List<Tool<?, ?>> allowedTools = filterAllowedTools(config);

        // 获取或创建会话历史
        String sessionId = generateSessionId(config, context);
        ConversationHistory history = sessionHistories.computeIfAbsent(
                sessionId,
                k -> new ConversationHistory()
        );

        return new ToolCallAgent(config, allowedTools, history, toolExecutor, llmProviderRegistry);
    }

    /**
     * 过滤Agent允许使用的工具
     */
    private List<Tool<?, ?>> filterAllowedTools(AgentConfig config) {
        List<Tool<?, ?>> allTools = toolExecutor.getAllTools();

        if (config.allowsAllTools()) {
            return new ArrayList<>(allTools);
        }

        return allTools.stream()
                .filter(tool -> config.allowsTool(tool.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId(AgentConfig config, ToolUseContext context) {
        String agentId = context.getAgentId();
        if (agentId != null && !agentId.isEmpty()) {
            return agentId;
        }
        return config.getAgentType() + "_default";
    }

    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        sessionHistories.remove(sessionId);
        log.info("清除会话历史: {}", sessionId);
    }

    /**
     * 获取会话历史
     */
    public ConversationHistory getSessionHistory(String sessionId) {
        return sessionHistories.get(sessionId);
    }

    /**
     * 获取所有会话ID
     */
    public Set<String> getAllSessionIds() {
        return new HashSet<>(sessionHistories.keySet());
    }

    /**
     * 会话历史类
     * 管理单个会话的消息历史和元数据
     */
    @Data
    public static class ConversationHistory {
        private final List<ChatMessage> messages = new ArrayList<>();

        public void addUserMessage(String content) {
            messages.add(new ChatMessage("user", content));
        }

        public void addAssistantMessage(String content) {
            messages.add(new ChatMessage("assistant", content));
        }

        public void addSystemMessage(String content) {
            messages.add(new ChatMessage("system", content));
        }

        public List<ChatMessage> getMessages() {
            return Collections.unmodifiableList(messages);
        }

        public void clear() {
            messages.clear();
        }

        public int getMessageCount() {
            return messages.size();
        }
    }

    /**
     * 聊天消息类
     */
    @Data
    public static class ChatMessage {
        private final String role;  // user, assistant, system
        private final String content;
        private final long timestamp;
        private final Map<String, Object> metadata;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
    }


    /**
     * Agent执行异常
     */
    public static class AgentExecutionException extends RuntimeException {
        public AgentExecutionException(String message) {
            super(message);
        }

        public AgentExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}