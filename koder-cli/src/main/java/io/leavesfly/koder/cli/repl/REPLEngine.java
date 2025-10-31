package io.leavesfly.koder.cli.repl;

import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandRegistry;
import io.leavesfly.koder.cli.command.CommandResult;
import io.leavesfly.koder.cli.terminal.TerminalRenderer;
import io.leavesfly.koder.tool.impl.BashTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.*;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REPL引擎
 * 负责读取-求值-打印循环的核心逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class REPLEngine {

    private final CommandRegistry commandRegistry;
    private final TerminalRenderer renderer;

    private final BashTool bashTool;

    private REPLSession session;
    private LineReader lineReader;
    private boolean running = false;

    /**
     * 启动REPL
     */
    public void start() {
        // 初始化会话
        session = new REPLSession(UUID.randomUUID().toString());

        // 创建行读取器
        lineReader = LineReaderBuilder.builder()
                .terminal(renderer.getTerminal())
                .build();

        // 显示欢迎信息
        showWelcome();

        // 主循环
        running = true;
        mainLoop();
    }

    /**
     * 主循环
     */
    private void mainLoop() {
        while (running) {
            try {
                // 读取输入
                String input = readInput();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                // 处理输入
                boolean shouldExit = processInput(input);

                if (shouldExit) {
                    break;
                }

            } catch (UserInterruptException e) {
                // Ctrl+C - 取消当前操作
                if (session.isLoading()) {
                    renderer.printWarning("\n操作已取消");
                    session.setLoading(false);
                } else {
                    renderer.printInfo("\n使用 /exit 或 Ctrl+D 退出");
                }
            } catch (EndOfFileException e) {
                // Ctrl+D - 退出
                break;
            } catch (Exception e) {
                log.error("REPL错误", e);
                renderer.printError("发生错误: " + e.getMessage());
            }
        }

        renderer.printSuccess("\n再见！");
    }

    /**
     * 读取用户输入
     */
    private String readInput() {
        String prompt = session.isLoading() ? "..." : "koder> ";
        return lineReader.readLine(prompt);
    }

    /**
     * 处理输入
     */
    private boolean processInput(String input) {
        // 检查是否为Bash模式（!开头）
        if (input.startsWith("!")) {
            String command = input.substring(1).trim();
            if (!command.isEmpty()) {
                return executeBashCommand(command);
            }
            return false;
        }

        // 检查是否为命令
        if (commandRegistry.isCommand(input)) {
            return executeCommand(input);
        } else {
            return handleUserMessage(input);
        }
    }

    /**
     * 执行Bash命令
     */
    private boolean executeBashCommand(String command) {
        // 显示命令回显
        renderer.println("! " + command);

        try {
            // 创建输入
            BashTool.Input input = BashTool.Input.builder()
                    .command(command)
                    .build();

            // 验证输入
            var validationResult = bashTool.validateInput(input, null);
            if (!validationResult.isResult()) {
                renderer.printError(validationResult.getMessage());
                return false;
            }

            // 执行命令
            bashTool.call(input, null)
                    .doOnNext(response -> {
                        if (response.getData() != null) {
                            BashTool.Output output = response.getData();

                            // 显示标准输出
                            if (!output.getStdout().isEmpty()) {
                                renderer.println(output.getStdout());
                            }

                            // 显示标准错误
                            if (!output.getStderr().isEmpty()) {
                                renderer.printError(output.getStderr());
                            }

                            // 显示退出码（如果非0）
                            if (output.getExitCode() != 0) {
                                renderer.printWarning("退出码: " + output.getExitCode());
                            }
                        }
                    })
                    .doOnError(error -> {
                        renderer.printError("命令执行失败: " + error.getMessage());
                        log.error("Bash命令执行失败", error);
                    })
                    .blockLast(); // 阻塞等待完成

        } catch (Exception e) {
            renderer.printError("发生错误: " + e.getMessage());
            log.error("执行Bash命令失败", e);
        }

        return false;
    }

    /**
     * 执行命令
     */
    private boolean executeCommand(String input) {
        CommandRegistry.CommandInput commandInput = commandRegistry.parse(input);

        if (commandInput == null) {
            renderer.printError("无效的命令格式");
            return false;
        }

        var commandOpt = commandRegistry.find(commandInput.getCommandName());

        if (commandOpt.isEmpty()) {
            renderer.printError("未知命令: /" + commandInput.getCommandName());
            renderer.printInfo("使用 /help 查看可用命令");
            return false;
        }

        // 构建命令上下文
        CommandContext context = CommandContext.builder()
                .args(commandInput.getArgs())
                .rawInput(input)
                .session(session)
                .output(new TerminalOutputImpl())
                .build();

        // 执行命令
        CommandResult result = commandOpt.get().execute(context);

        // 处理结果
        if (!result.isSuccess() && result.getMessage() != null) {
            renderer.printError(result.getMessage());
        }

        if (result.isShouldClearScreen()) {
            renderer.clearScreen();
            session.clearMessages();
        }

        return result.isShouldExit();
    }

    /**
     * 处理用户消息
     */
    private boolean handleUserMessage(String input) {
        // 设置加载状态
        session.setLoading(true);

        try {
            // 准备系统提示词
            String systemPrompt = buildSystemPrompt();

            // 显示思考提示
            renderer.printInfo("\n正在思考...");

            // 用于收集完整响应
            StringBuilder fullResponse = new StringBuilder();
            AtomicBoolean hasError = new AtomicBoolean(false);

            // todo 调用AI查询


        } catch (Exception e) {
            renderer.printError("\n发生错误: " + e.getMessage());
            log.error("处理用户消息失败", e);
        } finally {
            session.setLoading(false);
        }

        return false;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
                你是Koder，一个智能的AI编程助手。
                
                你的能力：
                - 理解和分析代码
                - 编写和修改代码文件
                - 执行shell命令
                - 搜索文件和内容
                - 提供编程建议
                
                当前工作目录: """ + session.getWorkingDirectory() + """
                
                请提供清晰、准确、有帮助的回答。
                """;
    }

    /**
     * 显示欢迎信息
     */
    private void showWelcome() {
        renderer.println("\n" +
            "╔══════════════════════════════════════════╗\n" +
            "║        Koder - AI编程助手 (Java版)      ║\n" +
            "╚══════════════════════════════════════════╝\n");
        renderer.printInfo("输入 /help 查看帮助");
        renderer.printInfo("输入 ! 开头可直接运行 shell 命令（如：!ls -la）");
        renderer.printInfo("输入 /exit 退出程序\n");
    }

    /**
     * 停止REPL
     */
    public void stop() {
        running = false;
    }

    /**
     * TerminalOutput实现
     */
    private class TerminalOutputImpl implements CommandContext.TerminalOutput {
        @Override
        public void println(String message) {
            renderer.println(message);
        }

        @Override
        public void success(String message) {
            renderer.printSuccess(message);
        }

        @Override
        public void error(String message) {
            renderer.printError(message);
        }

        @Override
        public void warning(String message) {
            renderer.printWarning(message);
        }
    }
}
