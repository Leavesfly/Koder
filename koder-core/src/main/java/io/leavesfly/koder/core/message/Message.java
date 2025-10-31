package io.leavesfly.koder.core.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.Instant;

/**
 * 消息基类
 * 使用Jackson多态支持不同类型的消息
 */
@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "role"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserMessage.class, name = "user"),
    @JsonSubTypes.Type(value = AssistantMessage.class, name = "assistant"),
    @JsonSubTypes.Type(value = ToolResultMessage.class, name = "tool")
})
public abstract class Message {
    
    /**
     * 消息角色
     */
    protected String role;
    
    /**
     * 消息时间戳
     */
    protected Instant timestamp;
    
    /**
     * 消息ID（可选）
     */
    protected String messageId;
    
    /**
     * 保护无参构造器，供子类和Jackson使用
     */
    protected Message() {
        this.timestamp = Instant.now();
    }
    
    protected Message(String role) {
        this.role = role;
        this.timestamp = Instant.now();
    }
}
