package io.leavesfly.koder.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具结果消息
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ToolResultMessage extends Message {
    
    /**
     * 关联的工具调用ID
     */
    @JsonProperty("toolUseId")
    private String toolUseId;
    
    /**
     * 工具输出内容
     */
    @JsonProperty("content")
    private Object content;
    
    /**
     * 是否为错误结果
     */
    @JsonProperty("isError")
    private boolean isError;
    
    @Builder
    public ToolResultMessage(String toolUseId, Object content, boolean isError, String messageId) {
        super("tool");
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = isError;
        this.messageId = messageId;
    }
    
    /**
     * 创建成功的工具结果
     */
    public static ToolResultMessage success(String toolUseId, Object content) {
        return ToolResultMessage.builder()
            .toolUseId(toolUseId)
            .content(content)
            .isError(false)
            .build();
    }
    
    /**
     * 创建错误的工具结果
     */
    public static ToolResultMessage error(String toolUseId, String errorMessage) {
        return ToolResultMessage.builder()
            .toolUseId(toolUseId)
            .content(errorMessage)
            .isError(true)
            .build();
    }
}
