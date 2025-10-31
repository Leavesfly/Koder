package io.leavesfly.koder.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP JSON-RPC响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPResponse {

    /**
     * JSON-RPC版本
     */
    private String jsonrpc = "2.0";

    /**
     * 请求ID
     */
    private String id;

    /**
     * 结果（成功时）
     */
    private Object result;

    /**
     * 错误（失败时）
     */
    private MCPError error;

    /**
     * MCP错误
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MCPError {
        /**
         * 错误代码
         */
        private int code;

        /**
         * 错误消息
         */
        private String message;

        /**
         * 错误数据
         */
        private Object data;
    }
}
