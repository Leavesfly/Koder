package io.leavesfly.koder.mcp.client.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.koder.mcp.client.MCPClient;
import io.leavesfly.koder.mcp.config.MCPServerConfig;
import io.leavesfly.koder.mcp.protocol.MCPRequest;
import io.leavesfly.koder.mcp.protocol.MCPResponse;
import io.leavesfly.koder.mcp.protocol.MCPTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE传输MCP客户端
 * 通过HTTP SSE与MCP服务器通信
 */
@Slf4j
public class SSEMCPClient implements MCPClient {

    private final MCPServerConfig config;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong requestIdCounter = new AtomicLong(0);

    private final Map<String, Sinks.One<MCPResponse>> pendingRequests = new ConcurrentHashMap<>();

    public SSEMCPClient(MCPServerConfig config, ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl(config.getUrl()).build();
    }

    @Override
    public Mono<Void> connect() {
        return Mono.fromRunnable(() -> {
            log.info("连接到MCP服务器（SSE）: {} ({})", config.getName(), config.getUrl());
            connected.set(true);
            log.info("MCP服务器（SSE）连接成功: {}", config.getName());
        });
    }

    @Override
    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            log.info("断开MCP服务器（SSE）连接: {}", config.getName());
            connected.set(false);

            // 清理待处理请求
            pendingRequests.forEach((id, sink) ->
                    sink.tryEmitError(new RuntimeException("连接已关闭"))
            );
            pendingRequests.clear();

            log.info("MCP服务器（SSE）连接已断开: {}", config.getName());
        });
    }

    @Override
    public Mono<MCPResponse> sendRequest(MCPRequest request) {
        if (!connected.get()) {
            return Mono.error(new IllegalStateException("客户端未连接"));
        }

        // 生成请求ID
        if (request.getId() == null) {
            request.setId(String.valueOf(requestIdCounter.incrementAndGet()));
        }

        log.debug("发送MCP请求（SSE）: {} -> {}", config.getName(), request.getMethod());

        // 发送HTTP POST请求
        return webClient.post()
                .uri("/message")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MCPResponse.class)
                .doOnNext(response -> 
                    log.debug("收到MCP响应（SSE）: {}", response.getId())
                )
                .doOnError(error ->
                    log.error("MCP请求失败（SSE）: {}", request.getMethod(), error)
                );
    }

    @Override
    public Mono<List<MCPTool>> listTools() {
        MCPRequest request = MCPRequest.builder()
                .method("tools/list")
                .params(Collections.emptyMap())
                .build();

        return sendRequest(request)
                .map(response -> {
                    if (response.getError() != null) {
                        throw new RuntimeException(
                                "列出工具失败: " + response.getError().getMessage()
                        );
                    }

                    // 解析工具列表
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.getResult();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> toolsList =
                            (List<Map<String, Object>>) result.get("tools");

                    return toolsList.stream()
                            .map(toolData -> MCPTool.builder()
                                    .name((String) toolData.get("name"))
                                    .description((String) toolData.get("description"))
                                    .inputSchema((Map<String, Object>) toolData.get("inputSchema"))
                                    .build())
                            .toList();
                });
    }

    @Override
    public Mono<Map<String, Object>> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        MCPRequest request = MCPRequest.builder()
                .method("tools/call")
                .params(params)
                .build();

        return sendRequest(request)
                .map(response -> {
                    if (response.getError() != null) {
                        throw new RuntimeException(
                                "调用工具失败: " + response.getError().getMessage()
                        );
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.getResult();
                    return result;
                });
    }

    @Override
    public MCPServerConfig getConfig() {
        return config;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}
