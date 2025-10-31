package io.leavesfly.koder.tool.executor;

import io.leavesfly.koder.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具执行引擎
 * 负责工具的注册、管理和执行
 */
@Slf4j
@Service
public class ToolExecutor {

    /**
     * 已注册的工具（按名称索引）
     */
    private final Map<String, Tool<?, ?>> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    public void registerTool(Tool<?, ?> tool) {
        if (tool.isEnabled()) {
            tools.put(tool.getName(), tool);
            log.info("注册工具: {}", tool.getName());
        }
    }

    /**
     * 批量注册工具
     *
     * @param toolList 工具列表
     */
    public void registerTools(List<Tool<?, ?>> toolList) {
        toolList.forEach(this::registerTool);
    }

    /**
     * 获取工具
     *
     * @param name 工具名称
     * @return 工具实例（可能为null）
     */
    public Tool<?, ?> getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有已注册的工具
     *
     * @return 工具列表
     */
    public List<Tool<?, ?>> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 获取所有只读工具
     *
     * @return 只读工具列表
     */
    public List<Tool<?, ?>> getReadOnlyTools() {
        return tools.values().stream()
                .filter(Tool::isReadOnly)
                .toList();
    }

    /**
     * 执行工具调用
     *
     * @param toolName 工具名称
     * @param input    输入参数
     * @param context  执行上下文
     * @param <I>      输入类型
     * @param <O>      输出类型
     * @return 执行结果流
     */
    @SuppressWarnings("unchecked")
    public <I, O> Flux<ToolResponse<O>> execute(
            String toolName,
            I input,
            ToolUseContext context) {

        Tool<I, O> tool = (Tool<I, O>) tools.get(toolName);

        if (tool == null) {
            return Flux.error(new ToolNotFoundException("工具不存在: " + toolName));
        }

        // 验证输入
        ValidationResult validation = tool.validateInput(input, context);
        if (!validation.isResult()) {
            return Flux.error(new ToolValidationException(
                    validation.getMessage(),
                    validation.getErrorCode()
            ));
        }

        // 检查是否中断
        if (context.getAbortController() != null &&
                context.getAbortController().isAborted()) {
            return Flux.error(new ToolAbortedException("工具执行已中断"));
        }

        try {
            return tool.call(input, context)
                    .doOnSubscribe(sub -> log.debug("开始执行工具: {}", toolName))
                    .doOnComplete(() -> log.debug("工具执行完成: {}", toolName))
                    .doOnError(error -> log.error("工具执行失败: {}, 错误: {}",
                            toolName, error.getMessage()));
        } catch (Exception e) {
            return Flux.error(new ToolExecutionException(
                    "工具执行异常: " + e.getMessage(), e));
        }
    }

    /**
     * 执行工具并等待结果
     *
     * @param toolName 工具名称
     * @param input    输入参数
     * @param context  执行上下文
     * @param <I>      输入类型
     * @param <O>      输出类型
     * @return 最终结果
     */
    public <I, O> Mono<O> executeAndGetResult(
            String toolName,
            I input,
            ToolUseContext context) {

        return execute(toolName, input, context)
                .filter(response -> response.getType() == ToolResponse.ResponseType.RESULT)
                .map(response -> (O) response.getData())
                .last();
    }

    /**
     * 获取工具的JSON Schema
     *
     * @param toolName 工具名称
     * @return JSON Schema
     */
    public Map<String, Object> getToolSchema(String toolName) {
        Tool<?, ?> tool = tools.get(toolName);
        if (tool == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("name", tool.getName());
        schema.put("description", tool.getDescription());
        schema.put("input_schema", tool.getInputSchema());

        return schema;
    }

    /**
     * 获取所有工具的Schema列表
     *
     * @return Schema列表
     */
    public List<Map<String, Object>> getAllToolSchemas() {
        return tools.values().stream()
                .map(tool -> getToolSchema(tool.getName()))
                .toList();
    }

    /**
     * 工具未找到异常
     */
    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * 工具验证异常
     */
    public static class ToolValidationException extends RuntimeException {
        private final Integer errorCode;

        public ToolValidationException(String message, Integer errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public Integer getErrorCode() {
            return errorCode;
        }
    }

    /**
     * 工具执行异常
     */
    public static class ToolExecutionException extends RuntimeException {
        public ToolExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 工具中断异常
     */
    public static class ToolAbortedException extends RuntimeException {
        public ToolAbortedException(String message) {
            super(message);
        }
    }
}
