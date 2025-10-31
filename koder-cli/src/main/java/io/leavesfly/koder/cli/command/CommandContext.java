package io.leavesfly.koder.cli.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 命令执行上下文
 * 包含命令执行所需的全部信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandContext {

    /**
     * 命令参数列表
     */
    private List<String> args;

    /**
     * 原始输入字符串
     */
    private String rawInput;

    /**
     * REPL会话（可选）
     */
    private Object session;

    /**
     * 终端输出接口
     */
    private TerminalOutput output;

    /**
     * 终端输出接口
     */
    public interface TerminalOutput {
        /**
         * 输出普通信息
         */
        void println(String message);

        /**
         * 输出成功信息
         */
        void success(String message);

        /**
         * 输出错误信息
         */
        void error(String message);

        /**
         * 输出警告信息
         */
        void warning(String message);
    }
}
