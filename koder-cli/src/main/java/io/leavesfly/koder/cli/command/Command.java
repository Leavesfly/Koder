package io.leavesfly.koder.cli.command;

/**
 * 命令接口
 * 所有REPL命令的基础契约
 */
public interface Command {

    /**
     * 获取命令名称（不含/前缀）
     *
     * @return 命令名称
     */
    String getName();

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    String getDescription();

    /**
     * 获取命令用法说明
     *
     * @return 用法说明
     */
    default String getUsage() {
        return "/" + getName();
    }

    /**
     * 执行命令
     *
     * @param context 命令执行上下文
     * @return 执行结果
     */
    CommandResult execute(CommandContext context);

    /**
     * 命令是否需要AI会话上下文
     *
     * @return true表示需要
     */
    default boolean requiresSession() {
        return false;
    }
}
