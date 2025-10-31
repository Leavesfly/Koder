package io.leavesfly.koder.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具执行上下文
 * 包含工具执行所需的全部上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolUseContext {

    /**
     * 消息ID（用于追踪）
     */
    private String messageId;

    /**
     * 代理ID（如果是代理调用）
     */
    private String agentId;

    /**
     * 是否为安全模式
     */
    private Boolean safeMode;

    /**
     * 中断控制器（用于取消长时间运行的操作）
     */
    private AbortController abortController;

    /**
     * 文件读取时间戳记录
     * key: 文件路径, value: 时间戳
     */
    private Map<String, Long> readFileTimestamps;

    /**
     * 额外选项
     */
    private ToolOptions options;

    /**
     * GPT-5 响应状态管理
     */
    private ResponseState responseState;

    /**
     * 工具执行选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolOptions {
        /**
         * 自定义命令列表
         */
        private Object[] commands;

        /**
         * 可用工具列表
         */
        private Object[] tools;

        /**
         * 是否详细模式
         */
        private Boolean verbose;

        /**
         * 慢速但强大的模型
         */
        private String slowAndCapableModel;

        /**
         * 是否安全模式
         */
        private Boolean safeMode;

        /**
         * Fork编号
         */
        private Integer forkNumber;

        /**
         * 消息日志名称
         */
        private String messageLogName;

        /**
         * 最大思考Token数
         */
        private Integer maxThinkingTokens;

        /**
         * 是否为Koding请求
         */
        private Boolean isKodingRequest;

        /**
         * Koding上下文
         */
        private String kodingContext;

        /**
         * 是否为自定义命令
         */
        private Boolean isCustomCommand;
    }

    /**
     * 响应状态（用于GPT-5等）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseState {
        /**
         * 上一个响应ID
         */
        private String previousResponseId;

        /**
         * 会话ID
         */
        private String conversationId;
    }
}
