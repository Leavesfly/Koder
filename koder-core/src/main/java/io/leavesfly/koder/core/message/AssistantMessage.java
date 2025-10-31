package io.leavesfly.koder.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 助手消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class AssistantMessage extends Message {
    
    /**
     * 文本内容
     */
    @JsonProperty("content")
    private String content;
    
    /**
     * 工具调用列表
     */
    @JsonProperty("toolUses")
    @Builder.Default
    private List<ToolUse> toolUses = new ArrayList<>();
    
    /**
     * 思考过程内容（可选）
     */
    @JsonProperty("thinkingContent")
    private String thinkingContent;
    
    /**
     * 停止原因
     */
    @JsonProperty("stopReason")
    private String stopReason;
    
    public AssistantMessage(String content, List<ToolUse> toolUses, String thinkingContent, String stopReason, String messageId) {
        super("assistant");
        this.content = content;
        this.toolUses = toolUses != null ? toolUses : new ArrayList<>();
        this.thinkingContent = thinkingContent;
        this.stopReason = stopReason;
        this.messageId = messageId;
    }
    
    /**
     * 便捷构造方法
     */
    public static AssistantMessage of(String content) {
        return AssistantMessage.builder()
            .content(content)
            .build();
    }
    
    /**
     * 是否包含工具调用
     */
    public boolean hasToolUses() {
        return toolUses != null && !toolUses.isEmpty();
    }
}
