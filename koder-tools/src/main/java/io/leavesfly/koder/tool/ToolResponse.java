package io.leavesfly.koder.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具响应（流式传输的数据块）
 *
 * @param <O> 输出类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResponse<O> {

    /**
     * 响应类型
     */
    private ResponseType type;

    /**
     * 结果数据（type=RESULT时有效）
     */
    private O data;

    /**
     * 给助手的结果（可选，覆盖默认渲染）
     */
    private Object resultForAssistant;

    /**
     * 进度内容（type=PROGRESS时有效）
     */
    private Object content;

    /**
     * 标准化消息列表（可选）
     */
    private Object[] normalizedMessages;

    /**
     * 工具列表（可选）
     */
    private Object[] tools;

    /**
     * 响应类型枚举
     */
    public enum ResponseType {
        /**
         * 最终结果
         */
        RESULT,

        /**
         * 进度更新
         */
        PROGRESS
    }

    /**
     * 创建结果响应
     *
     * @param data   结果数据
     * @param <O>    输出类型
     * @return 响应对象
     */
    public static <O> ToolResponse<O> result(O data) {
        return ToolResponse.<O>builder()
                .type(ResponseType.RESULT)
                .data(data)
                .build();
    }

    /**
     * 创建结果响应（包含自定义助手结果）
     *
     * @param data               结果数据
     * @param resultForAssistant 给助手的结果
     * @param <O>                输出类型
     * @return 响应对象
     */
    public static <O> ToolResponse<O> result(O data, Object resultForAssistant) {
        return ToolResponse.<O>builder()
                .type(ResponseType.RESULT)
                .data(data)
                .resultForAssistant(resultForAssistant)
                .build();
    }

    /**
     * 创建进度响应
     *
     * @param content 进度内容
     * @param <O>     输出类型
     * @return 响应对象
     */
    public static <O> ToolResponse<O> progress(Object content) {
        return ToolResponse.<O>builder()
                .type(ResponseType.PROGRESS)
                .content(content)
                .build();
    }
}
