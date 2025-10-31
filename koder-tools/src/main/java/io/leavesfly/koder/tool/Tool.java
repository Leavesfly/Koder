package io.leavesfly.koder.tool;

import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 工具接口 - Koder扩展工具系统的核心契约
 * 所有工具实现必须遵循此接口规范
 *
 * @param <I> 输入参数类型
 * @param <O> 输出结果类型
 */
public interface Tool<I, O> {

    /**
     * 工具名称（唯一标识）
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 工具描述（向AI模型说明工具的功能）
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取工具的系统提示词
     * 用于指导AI模型如何正确使用该工具
     *
     * @param safeMode 是否为安全模式
     * @return 提示词内容
     */
    String getPrompt(boolean safeMode);

    /**
     * 工具的输入参数Schema（JSON Schema格式）
     *
     * @return JSON Schema对象
     */
    Map<String, Object> getInputSchema();

    /**
     * 获取用户友好的工具名称
     *
     * @return 展示名称
     */
    default String getUserFacingName() {
        return getName();
    }

    /**
     * 工具是否启用
     *
     * @return true表示启用，false表示禁用
     */
    boolean isEnabled();

    /**
     * 工具是否为只读操作
     * 只读工具不会修改系统状态或文件
     *
     * @return true表示只读，false表示可能有副作用
     */
    boolean isReadOnly();

    /**
     * 工具是否支持并发安全执行
     *
     * @return true表示可以安全并发，false表示需要串行执行
     */
    boolean isConcurrencySafe();

    /**
     * 工具执行是否需要用户权限确认
     *
     * @param input 工具输入参数
     * @return true表示需要权限，false表示不需要
     */
    boolean needsPermissions(I input);

    /**
     * 验证输入参数的合法性
     *
     * @param input   工具输入
     * @param context 执行上下文
     * @return 验证结果
     */
    ValidationResult validateInput(I input, ToolUseContext context);

    /**
     * 渲染工具调用消息（向用户展示工具调用信息）
     *
     * @param input   工具输入
     * @param verbose 是否详细模式
     * @return 格式化的消息字符串
     */
    String renderToolUseMessage(I input, boolean verbose);

    /**
     * 渲染工具执行结果（向用户展示）
     *
     * @param output 工具输出
     * @return 格式化的结果字符串
     */
    String renderToolResultMessage(O output);

    /**
     * 渲染给AI助手的结果（向模型返回）
     *
     * @param output 工具输出
     * @return 格式化的结果内容（可能是字符串或结构化数据）
     */
    Object renderResultForAssistant(O output);

    /**
     * 执行工具调用（流式异步执行）
     * 
     * @param input   工具输入参数
     * @param context 执行上下文
     * @return 流式响应（可能包含进度更新和最终结果）
     */
    Flux<ToolResponse<O>> call(I input, ToolUseContext context);
}
