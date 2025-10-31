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
import java.util.*;
import java.util.regex.Pattern;

/**
 * 多重编辑工具 - 对单个文件进行多次原子性编辑
 */
@Slf4j
@Component
public class MultiEditTool extends AbstractTool<MultiEditTool.Input, MultiEditTool.Output> {

    @Override
    public String getName() {
        return "MultiEdit";
    }

    @Override
    public String getDescription() {
        return "对单个文件进行多次编辑操作，所有编辑作为一个原子操作执行";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                多重编辑工具 - 批量修改单个文件
                
                用法：
                - file_path: 要修改的文件的绝对路径（必需）
                - edits: 编辑操作数组，按顺序执行（必需）
                  - old_string: 要替换的文本
                  - new_string: 替换后的文本
                  - replace_all: 是否替换所有匹配（默认 false）
                
                何时使用：
                - 需要对同一文件进行多处修改
                - 所有修改必须作为一个事务执行（全部成功或全部失败）
                - 避免多次读写文件的开销
                
                注意事项：
                - 编辑按数组顺序依次执行
                - 如果任何一个编辑失败，整个操作都会回滚
                - old_string 必须在文件中存在（除非是创建新文件）
                - 对于 Jupyter Notebook (.ipynb)，请使用 NotebookEditCell 工具
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "file_path", Map.of(
                                "type", "string",
                                "description", "要修改的文件的绝对路径"
                        ),
                        "edits", Map.of(
                                "type", "array",
                                "description", "编辑操作数组",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "old_string", Map.of("type", "string", "description", "要替换的文本"),
                                                "new_string", Map.of("type", "string", "description", "替换后的文本"),
                                                "replace_all", Map.of(
                                                        "type", "boolean",
                                                        "description", "是否替换所有匹配（默认 false）",
                                                        "default", false
                                                )
                                        ),
                                        "required", List.of("old_string", "new_string")
                                )
                        )
                ),
                "required", List.of("file_path", "edits")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        return false; // 修改文件，不支持并发
    }

    @Override
    public boolean needsPermissions(Input input) {
        return true; // 需要文件写入权限
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        if (verbose) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("对 %s 进行 %d 处编辑:\n", input.filePath, input.edits.size()));
            for (int i = 0; i < input.edits.size(); i++) {
                Edit edit = input.edits.get(i);
                String oldPreview = truncate(edit.oldString, 50);
                String newPreview = truncate(edit.newString, 50);
                sb.append(String.format("%d. 替换 \"%s\" 为 \"%s\"%s\n",
                        i + 1, oldPreview, newPreview,
                        edit.replaceAll ? " (所有匹配)" : ""));
            }
            return sb.toString();
        }
        return String.format("对 %s 进行 %d 处编辑", input.filePath, input.edits.size());
    }

    @Override
    public String renderToolResultMessage(Output output) {
        if (!output.success) {
            return "❌ 编辑失败: " + output.error;
        }
        return String.format("✅ 成功应用 %d 处编辑到 %s",
                output.editsApplied, output.filePath);
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Path filePath = Paths.get(input.filePath);
                boolean fileExists = Files.exists(filePath);
                
                // 读取当前内容（新文件则为空）
                String currentContent = "";
                if (fileExists) {
                    currentContent = Files.readString(filePath);
                } else {
                    // 确保父目录存在
                    Path parentDir = filePath.getParent();
                    if (parentDir != null && !Files.exists(parentDir)) {
                        Files.createDirectories(parentDir);
                    }
                }
                
                // 按顺序应用所有编辑
                String modifiedContent = currentContent;
                List<AppliedEdit> appliedEdits = new ArrayList<>();
                
                for (int i = 0; i < input.edits.size(); i++) {
                    Edit edit = input.edits.get(i);
                    
                    try {
                        EditResult result = applyEdit(modifiedContent, edit);
                        modifiedContent = result.newContent;
                        appliedEdits.add(new AppliedEdit(
                                i + 1,
                                true,
                                truncate(edit.oldString, 100),
                                truncate(edit.newString, 100),
                                result.occurrences
                        ));
                    } catch (Exception e) {
                        // 任何编辑失败，终止整个操作
                        Output output = Output.builder()
                                .success(false)
                                .filePath(input.filePath)
                                .error(String.format("编辑 %d 失败: %s", i + 1, e.getMessage()))
                                .build();
                        sink.next(ToolResponse.result(output));
                        sink.complete();
                        return;
                    }
                }
                
                // 写入修改后的内容
                Files.writeString(filePath, modifiedContent);
                
                log.info("成功应用 {} 处编辑到文件: {}", appliedEdits.size(), input.filePath);
                
                Output output = Output.builder()
                        .success(true)
                        .filePath(input.filePath)
                        .wasNewFile(!fileExists)
                        .editsApplied(appliedEdits.size())
                        .totalEdits(input.edits.size())
                        .summary(String.format("成功应用 %d 处编辑到 %s",
                                appliedEdits.size(), input.filePath))
                        .build();
                
                sink.next(ToolResponse.result(output));
                sink.complete();
                
            } catch (IOException e) {
                log.error("多重编辑失败", e);
                Output output = Output.builder()
                        .success(false)
                        .filePath(input.filePath)
                        .error("文件操作失败: " + e.getMessage())
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
            } catch (Exception e) {
                log.error("多重编辑失败", e);
                sink.error(new RuntimeException("多重编辑失败: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 应用单个编辑操作
     */
    private EditResult applyEdit(String content, Edit edit) {
        if (edit.replaceAll) {
            // 替换所有匹配
            String escaped = Pattern.quote(edit.oldString);
            Pattern pattern = Pattern.compile(escaped);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            String newContent = content.replaceAll(escaped, 
                    java.util.regex.Matcher.quoteReplacement(edit.newString));
            return new EditResult(newContent, count);
        } else {
            // 替换第一个匹配
            if (content.contains(edit.oldString)) {
                String newContent = content.replaceFirst(
                        Pattern.quote(edit.oldString),
                        java.util.regex.Matcher.quoteReplacement(edit.newString));
                return new EditResult(newContent, 1);
            } else {
                throw new IllegalArgumentException(
                        "未找到要替换的字符串: " + truncate(edit.oldString, 50));
            }
        }
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 编辑操作
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edit {
        private String oldString;
        private String newString;
        private Boolean replaceAll = false;
    }
    
    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String filePath;
        private List<Edit> edits;
    }
    
    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private boolean success;
        private String filePath;
        private Boolean wasNewFile;
        private Integer editsApplied;
        private Integer totalEdits;
        private String summary;
        private String error;
    }
    
    /**
     * 应用的编辑信息
     */
    @Data
    @AllArgsConstructor
    private static class AppliedEdit {
        private int editIndex;
        private boolean success;
        private String oldString;
        private String newString;
        private int occurrences;
    }
    
    /**
     * 编辑结果
     */
    @Data
    @AllArgsConstructor
    private static class EditResult {
        private String newContent;
        private int occurrences;
    }
}
