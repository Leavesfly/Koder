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
import java.util.stream.Stream;

/**
 * 目录列表工具
 * 列出目录内容，支持递归列出子目录
 */
@Slf4j
@Component
public class LSTool extends AbstractTool<LSTool.Input, LSTool.Output> {

    @Override
    public String getName() {
        return "List";
    }

    @Override
    public String getUserFacingName() {
        return "LS";
    }

    @Override
    public String getDescription() {
        return "列出目录内容。可以选择是否递归列出子目录。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                列出目录内容：
                - path: 目录路径（必需）
                - recursive: 是否递归列出（可选，默认false）
                - max_depth: 最大递归深度（可选，默认1）
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("path", "要列出的目录路径")
                .addBooleanProperty("recursive", "是否递归列出子目录")
                .addNumberProperty("max_depth", "最大递归深度（默认1）")
                .required("path")
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
        return false;
    }

    @Override
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        Path dirPath = Paths.get(input.path);

        if (!Files.exists(dirPath)) {
            return ValidationResult.failure("路径不存在: " + input.path);
        }

        if (!Files.isDirectory(dirPath)) {
            return ValidationResult.failure("路径不是目录: " + input.path);
        }

        if (!Files.isReadable(dirPath)) {
            return ValidationResult.failure("目录不可读: " + input.path);
        }

        return ValidationResult.success();
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("path: %s, recursive: %s",
                input.path, input.recursive != null && input.recursive);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("目录: %s\n", output.path));
        sb.append(String.format("文件: %d, 目录: %d\n", output.fileCount, output.dirCount));

        int displayCount = Math.min(output.entries.size(), 20);
        for (int i = 0; i < displayCount; i++) {
            Entry entry = output.entries.get(i);
            String prefix = entry.isDirectory ? "[DIR]  " : "[FILE] ";
            sb.append(String.format("  %s%s\n", prefix, entry.name));
        }

        if (output.entries.size() > displayCount) {
            sb.append(String.format("  ... 还有 %d 个条目", output.entries.size() - displayCount));
        }

        return sb.toString();
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Path dirPath = Paths.get(input.path);
                List<Entry> entries = new ArrayList<>();
                int fileCount = 0;
                int dirCount = 0;

                if (Boolean.TRUE.equals(input.recursive)) {
                    int maxDepth = input.maxDepth != null ? input.maxDepth : Integer.MAX_VALUE;

                    try (Stream<Path> paths = Files.walk(dirPath, maxDepth)) {
                        for (Path path : paths.toList()) {
                            if (path.equals(dirPath)) continue;

                            boolean isDir = Files.isDirectory(path);
                            Entry entry = Entry.builder()
                                    .name(dirPath.relativize(path).toString())
                                    .isDirectory(isDir)
                                    .size(isDir ? 0 : Files.size(path))
                                    .build();

                            entries.add(entry);
                            if (isDir) {
                                dirCount++;
                            } else {
                                fileCount++;
                            }
                        }
                    }
                } else {
                    try (Stream<Path> paths = Files.list(dirPath)) {
                        for (Path path : paths.toList()) {
                            boolean isDir = Files.isDirectory(path);
                            Entry entry = Entry.builder()
                                    .name(path.getFileName().toString())
                                    .isDirectory(isDir)
                                    .size(isDir ? 0 : Files.size(path))
                                    .build();

                            entries.add(entry);
                            if (isDir) {
                                dirCount++;
                            } else {
                                fileCount++;
                            }
                        }
                    }
                }

                // 排序：目录在前，文件在后，同类型按名称排序
                entries.sort((a, b) -> {
                    if (a.isDirectory != b.isDirectory) {
                        return a.isDirectory ? -1 : 1;
                    }
                    return a.name.compareTo(b.name);
                });

                Output output = Output.builder()
                        .path(input.path)
                        .entries(entries)
                        .fileCount(fileCount)
                        .dirCount(dirCount)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.debug("目录列表完成: {}, {} 个条目", input.path, entries.size());

            } catch (IOException e) {
                log.error("列出目录失败: {}", input.path, e);
                sink.error(new RuntimeException("列出目录失败: " + e.getMessage(), e));
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
         * 目录路径
         */
        private String path;

        /**
         * 是否递归
         */
        @Builder.Default
        private Boolean recursive = false;

        /**
         * 最大深度
         */
        private Integer maxDepth;
    }

    /**
     * 目录条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        /**
         * 名称
         */
        private String name;

        /**
         * 是否为目录
         */
        private boolean isDirectory;

        /**
         * 大小（字节）
         */
        private long size;
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
         * 目录路径
         */
        private String path;

        /**
         * 条目列表
         */
        private List<Entry> entries;

        /**
         * 文件数量
         */
        private int fileCount;

        /**
         * 目录数量
         */
        private int dirCount;
    }
}
