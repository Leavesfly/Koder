package io.leavesfly.koder.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户消息
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserMessage extends Message {
    
    /**
     * 消息内容
     */
    @JsonProperty("content")
    private String content;
    
    @Builder
    public UserMessage(String content, String messageId) {
        super("user");
        this.content = content;
        this.messageId = messageId;
    }
    
    /**
     * 便捷构造方法
     */
    public static UserMessage of(String content) {
        return UserMessage.builder()
            .content(content)
            .build();
    }
}
