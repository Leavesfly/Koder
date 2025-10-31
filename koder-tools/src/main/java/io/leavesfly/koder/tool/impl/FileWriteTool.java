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
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * 文件写入工具
 * 用于创建新文件或覆盖现有文件
 */
@Slf4j
@Component
public class FileWriteTool extends AbstractTool<FileWriteTool.Input, FileWriteTool.Output> {

    @Override
    public String getName() {
        return "Create";
    }

    @Override
    public String getDescription() {
        return "创建新文件或覆盖现有文件。可以指定是否在文件末尾添加换行符。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return "使用此工具创建新文件。如果文件已存在，将被覆盖。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("file_path", "文件的绝对路径")
                .addStringProperty("content", "文件内容")
                .addBooleanProperty("add_newline", "是否在文件末尾添加换行符（默认true）")
                .required("file_path", "content")
                .build();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        Path filePath = Paths.get(input.filePath);
        Path parentDir = filePath.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                return ValidationResult.failure("无法创建父目录: " + e.getMessage());
            }
        }

        return ValidationResult.success();
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("file_path: %s, content_length: %d bytes",
                input.filePath, input.content.length());
    }

    @Override
    public String renderToolResultMessage(Output output) {
        return String.format("文件已创建: %s (%d 字节)",
                output.filePath, output.bytesWritten);
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Path filePath = Paths.get(input.filePath);

                // 准备写入内容
                String content = input.content;
                if (input.addNewline != null && input.addNewline && !content.endsWith("\n")) {
                    content += "\n";
                }

                // 写入文件
                Files.writeString(filePath, content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                // 更新时间戳
                if (context.getReadFileTimestamps() != null) {
                    context.getReadFileTimestamps().put(
                            input.filePath,
                            Files.getLastModifiedTime(filePath).toMillis()
                    );
                }

                Output output = Output.builder()
                        .filePath(input.filePath)
                        .bytesWritten(content.getBytes().length)
                        .success(true)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.info("文件已创建: {}", input.filePath);

            } catch (IOException e) {
                log.error("写入文件失败: {}", input.filePath, e);
                sink.error(new RuntimeException("写入文件失败: " + e.getMessage(), e));
            }
        });
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
         * 文件内容
         */
        private String content;

        /**
         * 是否添加换行符
         */
        @Builder.Default
        private Boolean addNewline = true;
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
         * 写入字节数
         */
        private long bytesWritten;

        /**
         * 是否成功
         */
        private boolean success;
    }
}
