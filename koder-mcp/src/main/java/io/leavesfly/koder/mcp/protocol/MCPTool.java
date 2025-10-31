package io.leavesfly.koder.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP工具定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPTool {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 输入Schema（JSON Schema格式）
     */
    private Map<String, Object> inputSchema;
}
