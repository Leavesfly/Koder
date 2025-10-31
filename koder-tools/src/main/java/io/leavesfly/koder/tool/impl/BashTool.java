package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Bash命令执行工具
 * 在系统shell中执行命令
 */
@Slf4j
@Component
public class BashTool extends AbstractTool<BashTool.Input, BashTool.Output> {

    /**
     * 禁止执行的命令列表（安全限制）
     */
    private static final List<String> BANNED_COMMANDS = Arrays.asList(
            "rm", "rmdir", "del", "format",
            "shutdown", "reboot", "halt",
            "dd", "mkfs", "fdisk"
    );

    private static final int DEFAULT_TIMEOUT_MS = 120000; // 2分钟
    private static final int MAX_TIMEOUT_MS = 600000; // 10分钟
    private static final int MAX_OUTPUT_LINES = 1000;

    @Override
    public String getName() {
        return "Bash";
    }

    @Override
    public String getDescription() {
        return "在系统shell中执行命令。支持超时控制，可以中断长时间运行的命令。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                执行shell命令：
                - command: 要执行的命令（必需）
                - timeout: 超时时间（毫秒，可选，默认120000）
                
                注意：某些危险命令（如rm、shutdown等）被禁止执行。
                当前工作目录: """ + System.getProperty("user.dir");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("command", "要执行的shell命令")
                .addNumberProperty("timeout", "超时时间（毫秒，可选，最大600000）")
                .required("command")
                .build();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        return false;
    }

    @Override
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        String command = input.command.trim();

        // 检查禁止的命令
        String[] parts = command.split("\\s+");
        if (parts.length > 0) {
            String baseCmd = parts[0].toLowerCase();
            // 移除路径前缀
            if (baseCmd.contains("/")) {
                baseCmd = baseCmd.substring(baseCmd.lastIndexOf("/") + 1);
            }

            if (BANNED_COMMANDS.contains(baseCmd)) {
                return ValidationResult.failure(
                        String.format("命令 '%s' 因安全原因被禁止执行", baseCmd)
                );
            }
        }

        // 检查超时时间
        if (input.timeout != null && input.timeout > MAX_TIMEOUT_MS) {
            return ValidationResult.failure(
                    String.format("超时时间不能超过 %d 毫秒", MAX_TIMEOUT_MS)
            );
        }

        return ValidationResult.success();
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        // 清理HEREDOC模式的命令显示
        String command = input.command;
        if (command.contains("<<'EOF'")) {
            command = command.replaceAll("\\$\\(cat <<'EOF'\\n([\\s\\S]*?)\\nEOF\\n\\)", "\"$1\"");
        }
        return command;
    }

    @Override
    public String renderToolResultMessage(Output output) {
        StringBuilder sb = new StringBuilder();

        if (!output.stdout.isEmpty()) {
            sb.append("标准输出:\n").append(output.stdout);
            if (output.stdoutTruncated) {
                sb.append("\n... (输出被截断)");
            }
        }

        if (!output.stderr.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("标准错误:\n").append(output.stderr);
            if (output.stderrTruncated) {
                sb.append("\n... (错误输出被截断)");
            }
        }

        if (output.interrupted) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("<命令在完成前被中断>");
        }

        if (output.exitCode != 0) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(String.format("退出代码: %d", output.exitCode));
        }

        return sb.toString();
    }

    @Override
    public Object renderResultForAssistant(Output output) {
        StringBuilder result = new StringBuilder();

        if (!output.stdout.isEmpty()) {
            result.append(output.stdout);
        }

        if (!output.stderr.isEmpty()) {
            if (result.length() > 0) result.append("\n");
            result.append(output.stderr);
        }

        if (output.interrupted) {
            if (result.length() > 0) result.append("\n");
            result.append("<error>命令在完成前被中断</error>");
        }

        return result.toString().trim();
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            // 检查是否已中断
            if (context.getAbortController() != null &&
                    context.getAbortController().isAborted()) {
                Output output = Output.builder()
                        .stdout("")
                        .stderr("命令在执行前被取消")
                        .exitCode(-1)
                        .interrupted(true)
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
                return;
            }

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            try {
                CommandLine cmdLine;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    cmdLine = CommandLine.parse("cmd.exe /c " + input.command);
                } else {
                    cmdLine = CommandLine.parse("/bin/sh -c \"" + input.command.replace("\"", "\\\"") + "\"");
                }

                DefaultExecutor executor = DefaultExecutor.builder().get();
                executor.setWorkingDirectory(new java.io.File(System.getProperty("user.dir")));

                // 设置超时
                int timeout = input.timeout != null ? input.timeout : DEFAULT_TIMEOUT_MS;
                ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                        .setTimeout(java.time.Duration.ofMillis(timeout))
                        .get();
                executor.setWatchdog(watchdog);

                // 设置输出流
                PumpStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr);
                executor.setStreamHandler(streamHandler);

                // 设置退出值处理（允许非0退出）
                executor.setExitValues(null);

                // 执行命令
                int exitCode;
                boolean interrupted = false;

                try {
                    exitCode = executor.execute(cmdLine);
                } catch (ExecuteException e) {
                    exitCode = e.getExitValue();
                    interrupted = watchdog.killedProcess();
                }

                // 处理输出
                String stdoutStr = truncateOutput(stdout.toString());
                String stderrStr = truncateOutput(stderr.toString());
                boolean stdoutTruncated = stdout.toString().split("\n").length > MAX_OUTPUT_LINES;
                boolean stderrTruncated = stderr.toString().split("\n").length > MAX_OUTPUT_LINES;

                Output output = Output.builder()
                        .stdout(stdoutStr)
                        .stdoutTruncated(stdoutTruncated)
                        .stderr(stderrStr)
                        .stderrTruncated(stderrTruncated)
                        .exitCode(exitCode)
                        .interrupted(interrupted)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.info("命令执行完成: {}, 退出码: {}", input.command, exitCode);

            } catch (IOException e) {
                log.error("命令执行失败: {}", input.command, e);

                Output output = Output.builder()
                        .stdout(stdout.toString())
                        .stderr("命令执行失败: " + e.getMessage())
                        .exitCode(-1)
                        .interrupted(false)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();
            }
        });
    }

    /**
     * 截断输出到最大行数
     */
    private String truncateOutput(String output) {
        String[] lines = output.split("\n");
        if (lines.length <= MAX_OUTPUT_LINES) {
            return output;
        }

        return String.join("\n", Arrays.copyOfRange(lines, 0, MAX_OUTPUT_LINES));
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        /**
         * 命令
         */
        private String command;

        /**
         * 超时时间（毫秒）
         */
        private Integer timeout;
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        /**
         * 标准输出
         */
        private String stdout;

        /**
         * 标准输出是否被截断
         */
        private boolean stdoutTruncated;

        /**
         * 标准错误
         */
        private String stderr;

        /**
         * 标准错误是否被截断
         */
        private boolean stderrTruncated;

        /**
         * 退出代码
         */
        private int exitCode;

        /**
         * 是否被中断
         */
        private boolean interrupted;
    }
}
