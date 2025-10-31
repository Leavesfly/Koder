package io.leavesfly.koder.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型指针配置
 * 将不同用途映射到具体模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelPointers {
    
    /**
     * 主对话模型ID
     */
    @NotBlank(message = "主模型ID不能为空")
    @JsonProperty("main")
    private String main;
    
    /**
     * 任务工具模型ID
     */
    @NotBlank(message = "任务模型ID不能为空")
    @JsonProperty("task")
    private String task;
    
    /**
     * 推理模型ID
     */
    @NotBlank(message = "推理模型ID不能为空")
    @JsonProperty("reasoning")
    private String reasoning;
    
    /**
     * 快速响应模型ID
     */
    @NotBlank(message = "快速模型ID不能为空")
    @JsonProperty("quick")
    private String quick;
}
