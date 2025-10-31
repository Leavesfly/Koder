package io.leavesfly.koder.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP JSON-RPC请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPRequest {

    /**
     * JSON-RPC版本（固定为"2.0"）
     */
    private String jsonrpc = "2.0";

    /**
     * 请求ID
     */
    private String id;

    /**
     * 方法名
     */
    private String method;

    /**
     * 参数
     */
    private Map<String, Object> params;
}
