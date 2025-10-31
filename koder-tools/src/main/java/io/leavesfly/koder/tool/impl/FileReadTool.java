package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件读取工具
 * 支持读取文本文件、查看指定行范围等
 */
@Slf4j
@Component
public class FileReadTool extends AbstractTool<FileReadTool.Input, FileReadTool.Output> {

    private static final int MAX_OUTPUT_SIZE = 256 * 1024; // 256KB
    private static final int MAX_LINES_TO_RENDER = 5;

    @Override
    public String getName() {
        return "View";
    }

    @Override
    public String getUserFacingName() {
        return "Read";
    }

    @Override
    public String getDescription() {
        return "读取文件内容。可以读取整个文件，或通过offset和limit参数读取指定行范围。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return "使用此工具读取文件内容。如果文件过大，使用offset和limit参数分段读取。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("file_path", "要读取的文件的绝对路径")
                .addNumberProperty("offset", "起始行号（可选，从1开始）")
                .addNumberProperty("limit", "读取行数（可选）")
                .required("file_path")
                .build();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    @Override
    public boolean needsPermissions(Input input) {
        // 可以在这里集成权限检查
        return false;
    }

    @Override
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        Path filePath = Paths.get(input.filePath);

        if (!Files.exists(filePath)) {
            return ValidationResult.failure("文件不存在: " + input.filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            return ValidationResult.failure("路径不是文件: " + input.filePath);
        }

        if (!Files.isReadable(filePath)) {
            return ValidationResult.failure("文件不可读: " + input.filePath);
        }

        return ValidationResult.success();
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        StringBuilder sb = new StringBuilder();
        sb.append("file_path: ").append(input.filePath);
        if (input.offset != null) {
            sb.append(", offset: ").append(input.offset);
        }
        if (input.limit != null) {
            sb.append(", limit: ").append(input.limit);
        }
        return sb.toString();
    }

    @Override
    public String renderToolResultMessage(Output output) {
        if (output.totalLines <= MAX_LINES_TO_RENDER) {
            return output.content;
        }
        String[] lines = output.content.split("\n");
        String preview = String.join("\n",
                java.util.Arrays.copyOfRange(lines, 0,
                        Math.min(lines.length, MAX_LINES_TO_RENDER)));
        return preview + "\n... (+" + (output.totalLines - MAX_LINES_TO_RENDER) + " 行)";
    }

    @Override
    public Object renderResultForAssistant(Output output) {
        return addLineNumbers(output);
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Path filePath = Paths.get(input.filePath);

                // 更新文件读取时间戳
                if (context.getReadFileTimestamps() != null) {
                    context.getReadFileTimestamps().put(
                            input.filePath,
                            System.currentTimeMillis()
                    );
                }

                // 读取文件
                List<String> allLines = Files.readAllLines(filePath);
                int totalLines = allLines.size();

                // 应用offset和limit
                int startLine = input.offset != null ? input.offset - 1 : 0;
                int endLine = input.limit != null ?
                        Math.min(startLine + input.limit, totalLines) : totalLines;

                if (startLine < 0) startLine = 0;
                if (startLine >= totalLines) {
                    sink.error(new IllegalArgumentException("Offset超出文件行数"));
                    return;
                }

                List<String> lines = allLines.subList(startLine, endLine);
                String content = String.join("\n", lines);

                // 检查大小限制
                if (content.length() > MAX_OUTPUT_SIZE) {
                    sink.error(new IllegalArgumentException(
                            String.format("文件内容 (%dKB) 超过最大允许大小 (%dKB)。请使用offset和limit参数分段读取。",
                                    content.length() / 1024, MAX_OUTPUT_SIZE / 1024)
                    ));
                    return;
                }

                Output output = Output.builder()
                        .filePath(input.filePath)
                        .content(content)
                        .numLines(lines.size())
                        .startLine(startLine + 1)
                        .totalLines(totalLines)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

            } catch (IOException e) {
                log.error("读取文件失败: {}", input.filePath, e);
                sink.error(new RuntimeException("读取文件失败: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 为内容添加行号
     */
    private String addLineNumbers(Output output) {
        String[] lines = output.content.split("\n");
        StringBuilder sb = new StringBuilder();

        int startLine = output.startLine;
        for (int i = 0; i < lines.length; i++) {
            sb.append(String.format("%4d | %s\n", startLine + i, lines[i]));
        }

        return sb.toString();
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
         * 文件路径
         */
        private String filePath;

        /**
         * 起始行号（从1开始）
         */
        private Integer offset;

        /**
         * 读取行数
         */
        private Integer limit;
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
         * 文件路径
         */
        private String filePath;

        /**
         * 文件内容
         */
        private String content;

        /**
         * 读取的行数
         */
        private int numLines;

        /**
         * 起始行号
         */
        private int startLine;

        /**
         * 文件总行数
         */
        private int totalLines;
    }
}
