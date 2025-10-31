package io.leavesfly.koder.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolUse {
    
    /**
     * 工具调用ID
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * 工具名称
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 工具输入参数（JSON格式）
     */
    @JsonProperty("input")
    private JsonNode input;
    
    /**
     * 工具调用类型（默认为tool_use）
     */
    @JsonProperty("type")
    @Builder.Default
    private String type = "tool_use";
}
