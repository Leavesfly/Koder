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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Grep搜索工具
 * 使用正则表达式在文件中搜索内容
 */
@Slf4j
@Component
public class GrepTool extends AbstractTool<GrepTool.Input, GrepTool.Output> {

    private static final int MAX_MATCHES = 100;
    private static final int CONTEXT_LINES = 2;

    @Override
    public String getName() {
        return "Grep";
    }

    @Override
    public String getDescription() {
        return "在文件中搜索文本。支持正则表达式匹配，可以指定文件模式过滤。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                使用正则表达式在文件中搜索：
                - regex: 正则表达式模式（必需）
                - file_pattern: 文件Glob模式（可选，如 *.java）
                - base_path: 搜索基础路径（可选）
                - case_sensitive: 是否区分大小写（默认true）
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("regex", "正则表达式搜索模式")
                .addStringProperty("file_pattern", "文件Glob模式（可选）")
                .addStringProperty("base_path", "搜索基础路径（可选）")
                .addBooleanProperty("case_sensitive", "是否区分大小写（默认true）")
                .required("regex")
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
        return String.format("regex: %s, pattern: %s",
                input.regex, input.filePattern != null ? input.filePattern : "*");
    }

    @Override
    public String renderToolResultMessage(Output output) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个匹配:\n", output.matches.size()));

        int displayCount = Math.min(output.matches.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            Match match = output.matches.get(i);
            sb.append(String.format("  %s:%d: %s\n",
                    match.file, match.lineNumber, match.line.trim()));
        }

        if (output.matches.size() > displayCount) {
            sb.append(String.format("  ... 还有 %d 个匹配", output.matches.size() - displayCount));
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

                // 编译正则表达式
                int flags = Boolean.FALSE.equals(input.caseSensitive) ?
                        Pattern.CASE_INSENSITIVE : 0;
                Pattern pattern = Pattern.compile(input.regex, flags);

                // 准备文件匹配器
                PathMatcher fileMatcher = null;
                if (input.filePattern != null && !input.filePattern.isEmpty()) {
                    fileMatcher = FileSystems.getDefault()
                            .getPathMatcher("glob:" + input.filePattern);
                }

                List<Match> matches = new ArrayList<>();
                PathMatcher finalFileMatcher = fileMatcher;

                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                        try {
                            // 检查文件模式
                            if (finalFileMatcher != null) {
                                Path relativePath = startPath.relativize(file);
                                if (!finalFileMatcher.matches(relativePath) &&
                                        !finalFileMatcher.matches(file.getFileName())) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            // 搜索文件内容
                            List<String> lines = Files.readAllLines(file);
                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                Matcher matcher = pattern.matcher(line);

                                if (matcher.find()) {
                                    Match match = Match.builder()
                                            .file(file.toString())
                                            .lineNumber(i + 1)
                                            .line(line)
                                            .matchedText(matcher.group())
                                            .build();
                                    matches.add(match);

                                    if (matches.size() >= MAX_MATCHES) {
                                        return FileVisitResult.TERMINATE;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.warn("无法读取文件: {}", file, e);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
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
                        .matches(matches)
                        .count(matches.size())
                        .regex(input.regex)
                        .truncated(matches.size() >= MAX_MATCHES)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.debug("Grep搜索完成: 模式={}, 结果={}", input.regex, matches.size());

            } catch (IOException e) {
                log.error("Grep搜索失败: {}", input.regex, e);
                sink.error(new RuntimeException("Grep搜索失败: " + e.getMessage(), e));
            } catch (java.util.regex.PatternSyntaxException e) {
                sink.error(new IllegalArgumentException("无效的正则表达式: " + e.getMessage()));
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
         * 正则表达式
         */
        private String regex;

        /**
         * 文件模式
         */
        private String filePattern;

        /**
         * 基础路径
         */
        private String basePath;

        /**
         * 是否区分大小写
         */
        @Builder.Default
        private Boolean caseSensitive = true;
    }

    /**
     * 匹配结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
        /**
         * 文件路径
         */
        private String file;

        /**
         * 行号
         */
        private int lineNumber;

        /**
         * 行内容
         */
        private String line;

        /**
         * 匹配的文本
         */
        private String matchedText;
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
         * 匹配列表
         */
        private List<Match> matches;

        /**
         * 匹配数量
         */
        private int count;

        /**
         * 搜索的正则表达式
         */
        private String regex;

        /**
         * 是否被截断
         */
        private boolean truncated;
    }
}
