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
        //todo
        return null;
    }

}
