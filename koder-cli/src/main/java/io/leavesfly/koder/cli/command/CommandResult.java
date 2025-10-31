package io.leavesfly.koder.cli.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命令执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 是否需要退出REPL
     */
    @Builder.Default
    private boolean shouldExit = false;

    /**
     * 是否需要清屏
     */
    @Builder.Default
    private boolean shouldClearScreen = false;

    /**
     * 附加数据（可选）
     */
    private Object data;

    /**
     * 创建成功结果
     */
    public static CommandResult success(String message) {
        return CommandResult.builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static CommandResult failure(String message) {
        return CommandResult.builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * 创建退出结果
     */
    public static CommandResult exit() {
        return CommandResult.builder()
                .success(true)
                .shouldExit(true)
                .message("再见！")
                .build();
    }
}
