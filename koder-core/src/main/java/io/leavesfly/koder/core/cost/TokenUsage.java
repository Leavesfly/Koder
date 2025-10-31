package io.leavesfly.koder.core.cost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token使用统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {
    
    /**
     * 输入Token数
     */
    @Builder.Default
    private int inputTokens = 0;
    
    /**
     * 输出Token数
     */
    @Builder.Default
    private int outputTokens = 0;
    
    /**
     * 缓存创建Token数
     */
    @Builder.Default
    private int cacheCreationTokens = 0;
    
    /**
     * 缓存读取Token数
     */
    @Builder.Default
    private int cacheReadTokens = 0;
    
    /**
     * 总Token数
     */
    public int getTotalTokens() {
        return inputTokens + outputTokens + cacheCreationTokens + cacheReadTokens;
    }
    
    /**
     * 累加Token使用
     */
    public void add(TokenUsage other) {
        if (other != null) {
            this.inputTokens += other.inputTokens;
            this.outputTokens += other.outputTokens;
            this.cacheCreationTokens += other.cacheCreationTokens;
            this.cacheReadTokens += other.cacheReadTokens;
        }
    }
}
