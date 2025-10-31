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
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Glob模式文件搜索工具
 * 支持使用通配符模式搜索文件
 */
@Slf4j
@Component
public class GlobTool extends AbstractTool<GlobTool.Input, GlobTool.Output> {

    private static final int MAX_RESULTS = 1000;

    @Override
    public String getName() {
        return "Glob";
    }

    @Override
    public String getDescription() {
        return "使用Glob模式搜索文件。支持通配符如 *.java, **/*.txt 等。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                使用Glob模式搜索文件：
                - * 匹配任意字符（不包括/）
                - ** 匹配任意路径
                - ? 匹配单个字符
                - [] 匹配字符范围
                
                示例: *.java, src/**/*.ts, **/*.{java,kt}
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("pattern", "Glob匹配模式")
                .addStringProperty("base_path", "搜索的基础路径（可选，默认当前目录）")
                .required("pattern")
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
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("pattern: %s", input.pattern);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        int displayCount = Math.min(output.files.size(), 10);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个文件:\n", output.files.size()));
        for (int i = 0; i < displayCount; i++) {
            sb.append("  ").append(output.files.get(i)).append("\n");
        }
        if (output.files.size() > displayCount) {
            sb.append(String.format("  ... 还有 %d 个文件", output.files.size() - displayCount));
        }
        return sb.toString();
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                String basePath = input.basePath != null ? input.basePath : System.getProperty("user.dir");
                Path startPath = Paths.get(basePath);

                if (!Files.exists(startPath)) {
                    sink.error(new IllegalArgumentException("基础路径不存在: " + basePath));
                    return;
                }

                List<String> matchedFiles = new ArrayList<>();
                PathMatcher matcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + input.pattern);

                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                        Path relativePath = startPath.relativize(file);
                        if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                            matchedFiles.add(file.toString());
                            if (matchedFiles.size() >= MAX_RESULTS) {
                                return FileVisitResult.TERMINATE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
                        // 跳过常见的不需要搜索的目录
                        String dirName = dir.getFileName().toString();
                        if (dirName.startsWith(".") ||
                                dirName.equals("node_modules") ||
                                dirName.equals("target") ||
                                dirName.equals("build")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                Output output = Output.builder()
                        .files(matchedFiles)
                        .count(matchedFiles.size())
                        .pattern(input.pattern)
                        .truncated(matchedFiles.size() >= MAX_RESULTS)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.debug("Glob搜索完成: 模式={}, 结果={}", input.pattern, matchedFiles.size());

            } catch (IOException e) {
                log.error("Glob搜索失败: {}", input.pattern, e);
                sink.error(new RuntimeException("Glob搜索失败: " + e.getMessage(), e));
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
         * Glob模式
         */
        private String pattern;

        /**
         * 基础路径
         */
        private String basePath;
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
         * 匹配的文件列表
         */
        private List<String> files;

        /**
         * 文件数量
         */
        private int count;

        /**
         * 搜索模式
         */
        private String pattern;

        /**
         * 是否被截断（达到最大结果数）
         */
        private boolean truncated;
    }
}
