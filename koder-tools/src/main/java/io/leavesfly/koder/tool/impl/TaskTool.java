package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务管理工具
 * 支持创建、更新、查询任务列表
 */
@Slf4j
@Component
public class TaskTool extends AbstractTool<TaskTool.Input, TaskTool.Output> {

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,      // 待处理
        IN_PROGRESS,  // 进行中
        COMPLETE,     // 已完成
        CANCELLED,    // 已取消
        ERROR         // 错误
    }

    /**
     * 任务存储（内存）
     * 实际应用中应持久化到数据库
     */
    private static final Map<String, Task> TASKS = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "Task";
    }

    @Override
    public String getDescription() {
        return "管理任务列表。支持添加、更新、查询任务。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                任务管理工具：
                - action: add（添加任务）、update（更新任务）、list（列出任务）
                - task_id: 任务ID（更新时必需）
                - content: 任务内容（添加时必需）
                - status: 任务状态（PENDING, IN_PROGRESS, COMPLETE, CANCELLED, ERROR）
                - parent_id: 父任务ID（可选，用于创建子任务）
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of(
                                "type", "string",
                                "description", "操作类型：add, update, list",
                                "enum", List.of("add", "update", "list")
                        ),
                        "task_id", Map.of("type", "string", "description", "任务ID"),
                        "content", Map.of("type", "string", "description", "任务内容"),
                        "status", Map.of(
                                "type", "string",
                                "description", "任务状态",
                                "enum", List.of("PENDING", "IN_PROGRESS", "COMPLETE", "CANCELLED", "ERROR")
                        ),
                        "parent_id", Map.of("type", "string", "description", "父任务ID")
                ),
                "required", List.of("action")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean needsPermissions(Input input) {
        return false;
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("action: %s", input.action);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        return output.message;
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Output output = switch (input.action.toLowerCase()) {
                    case "add" -> addTask(input);
                    case "update" -> updateTask(input);
                    case "list" -> listTasks();
                    default -> Output.builder()
                            .success(false)
                            .message("未知操作: " + input.action)
                            .build();
                };

                sink.next(ToolResponse.result(output));
                sink.complete();

            } catch (Exception e) {
                log.error("任务操作失败: {}", input.action, e);
                Output output = Output.builder()
                        .success(false)
                        .message("操作失败: " + e.getMessage())
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
            }
        });
    }

    /**
     * 添加任务
     */
    private Output addTask(Input input) {
        if (input.content == null || input.content.isEmpty()) {
            return Output.builder()
                    .success(false)
                    .message("任务内容不能为空")
                    .build();
        }

        String taskId = input.taskId != null ? input.taskId : generateTaskId();
        TaskStatus status = input.status != null ?
                TaskStatus.valueOf(input.status) : TaskStatus.PENDING;

        Task task = Task.builder()
                .id(taskId)
                .content(input.content)
                .status(status)
                .parentId(input.parentId)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        TASKS.put(taskId, task);

        return Output.builder()
                .success(true)
                .message(String.format("任务已创建: %s", taskId))
                .taskId(taskId)
                .task(task)
                .build();
    }

    /**
     * 更新任务
     */
    private Output updateTask(Input input) {
        if (input.taskId == null) {
            return Output.builder()
                    .success(false)
                    .message("任务ID不能为空")
                    .build();
        }

        Task task = TASKS.get(input.taskId);
        if (task == null) {
            return Output.builder()
                    .success(false)
                    .message("任务不存在: " + input.taskId)
                    .build();
        }

        // 更新字段
        if (input.content != null) {
            task.setContent(input.content);
        }
        if (input.status != null) {
            task.setStatus(TaskStatus.valueOf(input.status));
        }
        task.setUpdatedAt(System.currentTimeMillis());

        TASKS.put(task.getId(), task);

        return Output.builder()
                .success(true)
                .message(String.format("任务已更新: %s", task.getId()))
                .taskId(task.getId())
                .task(task)
                .build();
    }

    /**
     * 列出所有任务
     */
    private Output listTasks() {
        List<Task> tasks = new ArrayList<>(TASKS.values());

        return Output.builder()
                .success(true)
                .message(String.format("共 %d 个任务", tasks.size()))
                .tasks(tasks)
                .build();
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" +
                (int) (Math.random() * 10000);
    }

    /**
     * 任务实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private String id;
        private String content;
        private TaskStatus status;
        private String parentId;
        private long createdAt;
        private long updatedAt;
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String action;
        private String taskId;
        private String content;
        private String status;
        private String parentId;
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
        private String message;
        private String taskId;
        private Task task;
        private List<Task> tasks;
    }
}
