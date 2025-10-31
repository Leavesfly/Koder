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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件编辑工具
 * 支持基于搜索-替换的文件修改操作
 */
@Slf4j
@Component
public class FileEditTool extends AbstractTool<FileEditTool.Input, FileEditTool.Output> {

    @Override
    public String getName() {
        return "Edit";
    }

    @Override
    public String getDescription() {
        return "通过搜索-替换方式编辑文件。支持批量替换操作，每个替换必须唯一匹配。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                使用此工具编辑文件。提供一个或多个替换操作：
                - original_text: 要替换的原始文本（必须在文件中唯一匹配）
                - new_text: 替换后的文本
                - replace_all: 是否替换所有匹配（默认false）
                
                重要：original_text必须完全匹配，包括空格和缩进。
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> replacementSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "original_text", Map.of("type", "string", "description", "要替换的原始文本"),
                        "new_text", Map.of("type", "string", "description", "替换后的文本"),
                        "replace_all", Map.of("type", "boolean", "description", "是否替换所有匹配")
                ),
                "required", List.of("original_text", "new_text")
        );

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "file_path", Map.of("type", "string", "description", "要编辑的文件路径"),
                        "replacements", Map.of(
                                "type", "array",
                                "items", replacementSchema,
                                "description", "替换操作列表"
                        )
                ),
                "required", List.of("file_path", "replacements")
        );
    }

    @Override
    public boolean isReadOnly() {
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

        if (!Files.isWritable(filePath)) {
            return ValidationResult.failure("文件不可写: " + input.filePath);
        }

        if (input.replacements == null || input.replacements.isEmpty()) {
            return ValidationResult.failure("替换列表不能为空");
        }

        return ValidationResult.success();
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("file_path: %s, replacements: %d",
                input.filePath, input.replacements.size());
    }

    @Override
    public String renderToolResultMessage(Output output) {
        return String.format("文件已编辑: %s (%d 处替换)",
                output.filePath, output.replacementCount);
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Path filePath = Paths.get(input.filePath);
                String content = Files.readString(filePath);
                String originalContent = content;
                int totalReplacements = 0;
                List<String> errors = new ArrayList<>();

                // 按顺序执行所有替换
                for (int i = 0; i < input.replacements.size(); i++) {
                    Replacement repl = input.replacements.get(i);

                    if (repl.originalText.equals(repl.newText)) {
                        errors.add(String.format("替换 %d: 原文本和新文本相同", i + 1));
                        continue;
                    }

                    // 检查是否存在匹配
                    int firstIndex = content.indexOf(repl.originalText);
                    if (firstIndex == -1) {
                        errors.add(String.format("替换 %d: 未找到匹配文本", i + 1));
                        continue;
                    }

                    // 如果不是replace_all，检查唯一性
                    if (!Boolean.TRUE.equals(repl.replaceAll)) {
                        int secondIndex = content.indexOf(repl.originalText, firstIndex + 1);
                        if (secondIndex != -1) {
                            errors.add(String.format("替换 %d: 找到多个匹配，请提供更多上下文使其唯一", i + 1));
                            continue;
                        }
                    }

                    // 执行替换
                    if (Boolean.TRUE.equals(repl.replaceAll)) {
                        int count = 0;
                        String temp = content;
                        while (temp.contains(repl.originalText)) {
                            temp = temp.replaceFirst(java.util.regex.Pattern.quote(repl.originalText),
                                    java.util.regex.Matcher.quoteReplacement(repl.newText));
                            count++;
                        }
                        content = temp;
                        totalReplacements += count;
                    } else {
                        content = content.replaceFirst(
                                java.util.regex.Pattern.quote(repl.originalText),
                                java.util.regex.Matcher.quoteReplacement(repl.newText)
                        );
                        totalReplacements++;
                    }
                }

                if (!errors.isEmpty()) {
                    sink.error(new RuntimeException("替换失败: " + String.join("; ", errors)));
                    return;
                }

                // 写入文件
                Files.writeString(filePath, content);

                // 更新时间戳
                if (context.getReadFileTimestamps() != null) {
                    context.getReadFileTimestamps().put(
                            input.filePath,
                            Files.getLastModifiedTime(filePath).toMillis()
                    );
                }

                Output output = Output.builder()
                        .filePath(input.filePath)
                        .replacementCount(totalReplacements)
                        .success(true)
                        .originalSize(originalContent.length())
                        .newSize(content.length())
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.info("文件已编辑: {}, {} 处替换", input.filePath, totalReplacements);

            } catch (IOException e) {
                log.error("编辑文件失败: {}", input.filePath, e);
                sink.error(new RuntimeException("编辑文件失败: " + e.getMessage(), e));
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
         * 替换操作列表
         */
        private List<Replacement> replacements;
    }

    /**
     * 替换操作
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Replacement {
        /**
         * 原始文本
         */
        private String originalText;

        /**
         * 新文本
         */
        private String newText;

        /**
         * 是否替换所有匹配
         */
        @Builder.Default
        private Boolean replaceAll = false;
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
         * 替换次数
         */
        private int replacementCount;

        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 原始文件大小
         */
        private int originalSize;

        /**
         * 新文件大小
         */
        private int newSize;
    }
}
