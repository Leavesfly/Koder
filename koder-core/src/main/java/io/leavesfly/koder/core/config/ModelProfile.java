package io.leavesfly.koder.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型配置类
 * 描述单个AI模型的完整配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProfile {
    
    /**
     * 用户友好的模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String name;
    
    /**
     * 提供商类型
     */
    @NotNull(message = "提供商类型不能为空")
    private ProviderType provider;
    
    /**
     * 实际的模型标识符（主键）
     */
    @NotBlank(message = "模型标识符不能为空")
    @JsonProperty("modelName")
    private String modelName;
    
    /**
     * 自定义API端点（可选）
     */
    @JsonProperty("baseURL")
    private String baseURL;
    
    /**
     * API密钥
     */
    @NotBlank(message = "API密钥不能为空")
    @JsonProperty("apiKey")
    private String apiKey;
    
    /**
     * 最大输出Token数
     */
    @Min(value = 1, message = "最大Token数必须大于0")
    @JsonProperty("maxTokens")
    private int maxTokens;
    
    /**
     * 上下文窗口大小
     */
    @Min(value = 1, message = "上下文窗口大小必须大于0")
    @JsonProperty("contextLength")
    private int contextLength;
    
    /**
     * 推理努力程度（GPT-5等模型使用）
     */
    @JsonProperty("reasoningEffort")
    private String reasoningEffort;
    
    /**
     * 是否启用
     */
    @JsonProperty("isActive")
    @Builder.Default
    private boolean isActive = true;
    
    /**
     * 创建时间戳
     */
    @JsonProperty("createdAt")
    private long createdAt;
    
    /**
     * 最后使用时间戳
     */
    @JsonProperty("lastUsed")
    private Long lastUsed;
    
    /**
     * 是否为GPT-5模型（自动检测）
     */
    @JsonProperty("isGPT5")
    private Boolean isGPT5;
    
    /**
     * 配置验证状态
     */
    @JsonProperty("validationStatus")
    private String validationStatus;
    
    /**
     * 最后验证时间戳
     */
    @JsonProperty("lastValidation")
    private Long lastValidation;
}
