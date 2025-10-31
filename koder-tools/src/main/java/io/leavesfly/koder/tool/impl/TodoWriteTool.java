package io.leavesfly.koder.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

/**
 * 任务列表管理工具
 */
@Slf4j
@Component
public class TodoWriteTool extends AbstractTool<TodoWriteTool.Input, TodoWriteTool.Output> {

    private static final String TODO_FILE = ".koder-todos.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "TodoWrite";
    }

    @Override
    public String getDescription() {
        return "更新和管理任务列表（TODO list）";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                任务列表管理工具
                
                用法：
                - todos: 任务数组，包含所有任务
                  - id: 任务的唯一标识符（必需）
                  - content: 任务描述（必需）
                  - status: 任务状态（pending/in_progress/completed）
                  - priority: 优先级（high/medium/low）
                
                状态说明：
                - pending: 待处理
                - in_progress: 进行中（同时只能有一个）
                - completed: 已完成
                
                优先级：
                - high: 高优先级
                - medium: 中等优先级
                - low: 低优先级
                
                最佳实践：
                - 使用有意义的 ID（如 "fix-bug-123"）
                - 保持任务描述简洁明确
                - 同时只有一个任务为 in_progress
                - 定期清理已完成的任务
                - 使用优先级管理任务顺序
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "todos", Map.of(
                                "type", "array",
                                "description", "更新后的任务列表",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "id", Map.of("type", "string", "description", "任务唯一标识符"),
                                                "content", Map.of("type", "string", "description", "任务描述"),
                                                "status", Map.of(
                                                        "type", "string",
                                                        "enum", List.of("pending", "in_progress", "completed"),
                                                        "description", "任务状态"
                                                ),
                                                "priority", Map.of(
                                                        "type", "string",
                                                        "enum", List.of("high", "medium", "low"),
                                                        "description", "优先级"
                                                )
                                        ),
                                        "required", List.of("id", "content", "status", "priority")
                                )
                        )
                ),
                "required", List.of("todos")
        );
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
    public boolean needsPermissions(Input input) {
        return false; // TodoWrite工具文件存储在用户目录，不需要额外权限
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("更新任务列表（%d 个任务）", input.todos.size());
    }

    @Override
    public String renderToolResultMessage(Output output) {
        if (!output.success) {
            return "❌ 更新失败: " + output.error;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("✅ 任务列表已更新\n\n"));
        
        if (output.todos.isEmpty()) {
            sb.append("当前没有任务\n");
        } else {
            // 按状态分组：completed, in_progress, pending
            List<TodoItem> sorted = new ArrayList<>(output.todos);
            sorted.sort(Comparator
                    .comparing((TodoItem t) -> getStatusOrder(t.status))
                    .thenComparing(t -> t.content));
            
            for (TodoItem todo : sorted) {
                String checkbox = switch (todo.status) {
                    case "completed" -> "☒";
                    case "in_progress" -> "☐";
                    default -> "☐";
                };
                
                String symbol = switch (todo.status) {
                    case "completed" -> "  ⎿ ";
                    case "in_progress" -> "  ⎿ ";
                    default -> "  ⎿ ";
                };
                
                sb.append(symbol).append(checkbox).append(" ").append(todo.content);
                
                if ("high".equals(todo.priority)) {
                    sb.append(" [!]");
                }
                
                sb.append("\n");
            }
        }
        
        sb.append(String.format("\n统计: %d 个任务 (%d 待处理, %d 进行中, %d 已完成)",
                output.total, output.pending, output.inProgress, output.completed));
        
        return sb.toString();
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                // 验证任务
                String validationError = validateTodos(input.todos);
                if (validationError != null) {
                    Output output = Output.builder()
                            .success(false)
                            .error(validationError)
                            .build();
                    sink.next(ToolResponse.result(output));
                    sink.complete();
                    return;
                }
                
                // 保存任务列表
                Path todoFile = getTodoFilePath();
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(todoFile.toFile(), input.todos);
                
                // 生成统计信息
                long pending = input.todos.stream()
                        .filter(t -> "pending".equals(t.status))
                        .count();
                long inProgress = input.todos.stream()
                        .filter(t -> "in_progress".equals(t.status))
                        .count();
                long completed = input.todos.stream()
                        .filter(t -> "completed".equals(t.status))
                        .count();
                
                log.info("任务列表已更新: {} 个任务 ({} 待处理, {} 进行中, {} 已完成)",
                        input.todos.size(), pending, inProgress, completed);
                
                Output output = Output.builder()
                        .success(true)
                        .todos(input.todos)
                        .total(input.todos.size())
                        .pending((int) pending)
                        .inProgress((int) inProgress)
                        .completed((int) completed)
                        .summary(String.format("成功更新 %d 个任务", input.todos.size()))
                        .build();
                
                sink.next(ToolResponse.result(output));
                sink.complete();
                
            } catch (IOException e) {
                log.error("保存任务列表失败", e);
                Output output = Output.builder()
                        .success(false)
                        .error("文件操作失败: " + e.getMessage())
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
            } catch (Exception e) {
                log.error("更新任务列表失败", e);
                sink.error(new RuntimeException("更新任务列表失败: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 验证任务列表
     */
    private String validateTodos(List<TodoItem> todos) {
        // 检查重复 ID
        Set<String> ids = new HashSet<>();
        for (TodoItem todo : todos) {
            if (!ids.add(todo.id)) {
                return "发现重复的任务 ID: " + todo.id;
            }
        }
        
        // 检查多个进行中的任务
        long inProgressCount = todos.stream()
                .filter(t -> "in_progress".equals(t.status))
                .count();
        if (inProgressCount > 1) {
            return "同时只能有一个任务处于 in_progress 状态";
        }
        
        // 检查每个任务
        for (TodoItem todo : todos) {
            if (todo.content == null || todo.content.trim().isEmpty()) {
                return String.format("任务 %s 的内容不能为空", todo.id);
            }
            if (!List.of("pending", "in_progress", "completed").contains(todo.status)) {
                return String.format("任务 %s 的状态无效: %s", todo.id, todo.status);
            }
            if (!List.of("high", "medium", "low").contains(todo.priority)) {
                return String.format("任务 %s 的优先级无效: %s", todo.id, todo.priority);
            }
        }
        
        return null;
    }
    
    /**
     * 获取 TODO 文件路径
     */
    private Path getTodoFilePath() {
        String userHome = System.getProperty("user.home");
        Path koderDir = Paths.get(userHome, ".koder");
        try {
            if (!Files.exists(koderDir)) {
                Files.createDirectories(koderDir);
            }
        } catch (IOException e) {
            log.warn("无法创建 .koder 目录", e);
        }
        return koderDir.resolve(TODO_FILE);
    }
    
    /**
     * 获取状态排序权重
     */
    private int getStatusOrder(String status) {
        return switch (status) {
            case "completed" -> 0;
            case "in_progress" -> 1;
            default -> 2; // pending
        };
    }
    
    /**
     * 任务项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoItem {
        private String id;
        private String content;
        private String status;
        private String priority;
    }
    
    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private List<TodoItem> todos;
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
        private List<TodoItem> todos;
        private Integer total;
        private Integer pending;
        private Integer inProgress;
        private Integer completed;
        private String summary;
        private String error;
    }
}
