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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆读取工具
 * 从记忆系统中检索信息
 */
@Slf4j
@Component
public class MemoryReadTool extends AbstractTool<MemoryReadTool.Input, MemoryReadTool.Output> {

    /**
     * 记忆存储（内存实现）
     * 实际应用应使用持久化存储（如数据库、向量数据库等）
     */
    private static final Map<String, Memory> MEMORY_STORE = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "ReadMemory";
    }

    @Override
    public String getDescription() {
        return "从记忆系统中检索信息。支持按关键词、标签或时间范围搜索。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                从记忆系统检索信息：
                - query: 搜索查询（可选）
                - tags: 标签列表（可选）
                - limit: 最大结果数（可选，默认10）
                
                如果不提供任何参数，将返回最近的记忆。
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "搜索查询"),
                        "tags", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "标签列表"
                        ),
                        "limit", Map.of("type", "number", "description", "最大结果数")
                )
        );
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
        StringBuilder sb = new StringBuilder();
        if (input.query != null) {
            sb.append("query: ").append(input.query);
        }
        if (input.tags != null && !input.tags.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("tags: ").append(String.join(",", input.tags));
        }
        return sb.toString();
    }

    @Override
    public String renderToolResultMessage(Output output) {
        return String.format("找到 %d 条记忆", output.memories.size());
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                int limit = input.limit != null ? input.limit : 10;
                List<Memory> allMemories = new ArrayList<>(MEMORY_STORE.values());

                // 过滤和排序
                List<Memory> filtered = allMemories.stream()
                        .filter(m -> matchesQuery(m, input.query, input.tags))
                        .sorted(Comparator.comparingLong(Memory::getTimestamp).reversed())
                        .limit(limit)
                        .collect(Collectors.toList());

                Output output = Output.builder()
                        .memories(filtered)
                        .count(filtered.size())
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.debug("记忆检索完成: 找到 {} 条", filtered.size());

            } catch (Exception e) {
                log.error("记忆检索失败", e);
                sink.error(new RuntimeException("记忆检索失败: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 检查记忆是否匹配查询
     */
    private boolean matchesQuery(Memory memory, String query, List<String> tags) {
        // 如果没有任何过滤条件，返回所有记忆
        if (query == null && (tags == null || tags.isEmpty())) {
            return true;
        }

        // 检查标签匹配
        if (tags != null && !tags.isEmpty()) {
            if (memory.getTags() == null || Collections.disjoint(memory.getTags(), tags)) {
                return false;
            }
        }

        // 检查查询匹配
        if (query != null && !query.isEmpty()) {
            String lowerQuery = query.toLowerCase();
            return memory.getContent().toLowerCase().contains(lowerQuery) ||
                    (memory.getTitle() != null && memory.getTitle().toLowerCase().contains(lowerQuery));
        }

        return true;
    }

    /**
     * 记忆实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Memory {
        private String id;
        private String title;
        private String content;
        private List<String> tags;
        private long timestamp;
        private Map<String, Object> metadata;
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String query;
        private List<String> tags;
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
        private List<Memory> memories;
        private int count;
    }

    /**
     * 获取记忆存储（用于MemoryWriteTool访问）
     */
    public static Map<String, Memory> getMemoryStore() {
        return MEMORY_STORE;
    }
}
