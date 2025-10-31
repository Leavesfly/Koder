package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 记忆写入工具
 * 向记忆系统中存储信息
 */
@Slf4j
@Component
public class MemoryWriteTool extends AbstractTool<MemoryWriteTool.Input, MemoryWriteTool.Output> {

    @Override
    public String getName() {
        return "WriteMemory";
    }

    @Override
    public String getDescription() {
        return "向记忆系统中存储信息。可以添加新记忆或更新现有记忆。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                向记忆系统存储信息：
                - action: save（保存新记忆）或 update（更新记忆）或 delete（删除记忆）
                - id: 记忆ID（更新或删除时必需）
                - title: 记忆标题（保存时可选）
                - content: 记忆内容（保存时必需）
                - tags: 标签列表（可选）
                
                记忆将被持久化，可以通过ReadMemory工具检索。
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of(
                                "type", "string",
                                "description", "操作类型",
                                "enum", List.of("save", "update", "delete")
                        ),
                        "id", Map.of("type", "string", "description", "记忆ID"),
                        "title", Map.of("type", "string", "description", "记忆标题"),
                        "content", Map.of("type", "string", "description", "记忆内容"),
                        "tags", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "标签列表"
                        )
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
        return String.format("action: %s, title: %s",
                input.action, input.title != null ? input.title : "(无标题)");
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
                    case "save" -> saveMemory(input);
                    case "update" -> updateMemory(input);
                    case "delete" -> deleteMemory(input);
                    default -> Output.builder()
                            .success(false)
                            .message("未知操作: " + input.action)
                            .build();
                };

                sink.next(ToolResponse.result(output));
                sink.complete();

            } catch (Exception e) {
                log.error("记忆操作失败: {}", input.action, e);
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
     * 保存记忆
     */
    private Output saveMemory(Input input) {
        if (input.content == null || input.content.isEmpty()) {
            return Output.builder()
                    .success(false)
                    .message("记忆内容不能为空")
                    .build();
        }

        String memoryId = input.id != null ? input.id : generateMemoryId();

        MemoryReadTool.Memory memory = MemoryReadTool.Memory.builder()
                .id(memoryId)
                .title(input.title)
                .content(input.content)
                .tags(input.tags != null ? new ArrayList<>(input.tags) : new ArrayList<>())
                .timestamp(System.currentTimeMillis())
                .metadata(new HashMap<>())
                .build();

        MemoryReadTool.getMemoryStore().put(memoryId, memory);

        return Output.builder()
                .success(true)
                .message(String.format("记忆已保存: %s", memoryId))
                .memoryId(memoryId)
                .build();
    }

    /**
     * 更新记忆
     */
    private Output updateMemory(Input input) {
        if (input.id == null) {
            return Output.builder()
                    .success(false)
                    .message("记忆ID不能为空")
                    .build();
        }

        MemoryReadTool.Memory memory = MemoryReadTool.getMemoryStore().get(input.id);
        if (memory == null) {
            return Output.builder()
                    .success(false)
                    .message("记忆不存在: " + input.id)
                    .build();
        }

        // 更新字段
        if (input.title != null) {
            memory.setTitle(input.title);
        }
        if (input.content != null) {
            memory.setContent(input.content);
        }
        if (input.tags != null) {
            memory.setTags(new ArrayList<>(input.tags));
        }
        memory.setTimestamp(System.currentTimeMillis());

        MemoryReadTool.getMemoryStore().put(memory.getId(), memory);

        return Output.builder()
                .success(true)
                .message(String.format("记忆已更新: %s", memory.getId()))
                .memoryId(memory.getId())
                .build();
    }

    /**
     * 删除记忆
     */
    private Output deleteMemory(Input input) {
        if (input.id == null) {
            return Output.builder()
                    .success(false)
                    .message("记忆ID不能为空")
                    .build();
        }

        MemoryReadTool.Memory removed = MemoryReadTool.getMemoryStore().remove(input.id);
        if (removed == null) {
            return Output.builder()
                    .success(false)
                    .message("记忆不存在: " + input.id)
                    .build();
        }

        return Output.builder()
                .success(true)
                .message(String.format("记忆已删除: %s", input.id))
                .memoryId(input.id)
                .build();
    }

    /**
     * 生成记忆ID
     */
    private String generateMemoryId() {
        return "mem_" + System.currentTimeMillis() + "_" +
                (int) (Math.random() * 10000);
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
        private String id;
        private String title;
        private String content;
        private List<String> tags;
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
        private String memoryId;
    }
}

