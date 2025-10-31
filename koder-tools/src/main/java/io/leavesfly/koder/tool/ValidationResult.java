package io.leavesfly.koder.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 验证是否通过
     */
    private boolean result;

    /**
     * 错误消息（如果验证失败）
     */
    private String message;

    /**
     * 错误代码
     */
    private Integer errorCode;

    /**
     * 附加元数据
     */
    private Map<String, Object> meta;

    /**
     * 创建成功的验证结果
     *
     * @return 成功结果
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .result(true)
                .build();
    }

    /**
     * 创建失败的验证结果
     *
     * @param message 错误消息
     * @return 失败结果
     */
    public static ValidationResult failure(String message) {
        return ValidationResult.builder()
                .result(false)
                .message(message)
                .build();
    }

    /**
     * 创建带错误代码的失败结果
     *
     * @param message   错误消息
     * @param errorCode 错误代码
     * @return 失败结果
     */
    public static ValidationResult failure(String message, int errorCode) {
        return ValidationResult.builder()
                .result(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
