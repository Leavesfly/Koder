package io.leavesfly.koder.mcp.client.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.koder.mcp.config.MCPServerConfig;
import io.leavesfly.koder.mcp.protocol.MCPRequest;
import io.leavesfly.koder.mcp.protocol.MCPResponse;
import io.leavesfly.koder.mcp.protocol.MCPTool;
import io.leavesfly.koder.mcp.client.MCPClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stdio传输MCP客户端
 * 通过标准输入输出与MCP服务器通信
 */
@Slf4j
public class StdioMCPClient implements MCPClient {

    private final MCPServerConfig config;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong requestIdCounter = new AtomicLong(0);
    
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private Thread readerThread;
    
    // 请求-响应映射
    private final Map<String, Sinks.One<MCPResponse>> pendingRequests = new ConcurrentHashMap<>();

    public StdioMCPClient(MCPServerConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> connect() {
        return Mono.fromRunnable(() -> {
            try {
                log.info("连接到MCP服务器: {} ({})", config.getName(), config.getCommand());

                // 构建进程
                ProcessBuilder pb = new ProcessBuilder();
                List<String> command = new ArrayList<>();
                command.add(config.getCommand());
                if (config.getArgs() != null) {
                    command.addAll(Arrays.asList(config.getArgs()));
                }
                pb.command(command);

                // 设置环境变量
                if (config.getEnv() != null) {
                    pb.environment().putAll(config.getEnv());
                }

                // 启动进程
                process = pb.start();

                // 创建IO流
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // 启动读取线程
                startReaderThread();

                connected.set(true);
                log.info("MCP服务器连接成功: {}", config.getName());

            } catch (Exception e) {
                log.error("连接MCP服务器失败: {}", config.getName(), e);
                throw new RuntimeException("连接失败", e);
            }
        });
    }

    @Override
    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            try {
                log.info("断开MCP服务器连接: {}", config.getName());

                connected.set(false);

                // 关闭IO流
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }

                // 停止读取线程
                if (readerThread != null && readerThread.isAlive()) {
                    readerThread.interrupt();
                }

                // 销毁进程
                if (process != null && process.isAlive()) {
                    process.destroy();
                    process.waitFor();
                }

                // 清理待处理请求
                pendingRequests.forEach((id, sink) -> 
                    sink.tryEmitError(new RuntimeException("连接已关闭"))
                );
                pendingRequests.clear();

                log.info("MCP服务器连接已断开: {}", config.getName());

            } catch (Exception e) {
                log.error("断开MCP服务器连接失败: {}", config.getName(), e);
            }
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

        // 创建响应Sink
        Sinks.One<MCPResponse> responseSink = Sinks.one();
        pendingRequests.put(request.getId(), responseSink);

        return Mono.fromRunnable(() -> {
            try {
                // 序列化请求
                String jsonRequest = objectMapper.writeValueAsString(request);
                
                log.debug("发送MCP请求: {} -> {}", config.getName(), jsonRequest);

                // 发送请求
                synchronized (writer) {
                    writer.write(jsonRequest);
                    writer.newLine();
                    writer.flush();
                }

            } catch (Exception e) {
                log.error("发送MCP请求失败", e);
                pendingRequests.remove(request.getId());
                responseSink.tryEmitError(e);
            }
        }).then(responseSink.asMono());
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

    /**
     * 启动读取线程
     */
    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (connected.get() && (line = reader.readLine()) != null) {
                    try {
                        log.debug("收到MCP响应: {} <- {}", config.getName(), line);

                        // 解析响应
                        MCPResponse response = objectMapper.readValue(line, MCPResponse.class);

                        // 找到对应的请求
                        Sinks.One<MCPResponse> sink = pendingRequests.remove(response.getId());
                        if (sink != null) {
                            sink.tryEmitValue(response);
                        } else {
                            log.warn("收到未知请求ID的响应: {}", response.getId());
                        }

                    } catch (Exception e) {
                        log.error("处理MCP响应失败: {}", line, e);
                    }
                }
            } catch (Exception e) {
                if (connected.get()) {
                    log.error("读取MCP响应失败", e);
                }
            } finally {
                // 连接断开时清理
                if (connected.get()) {
                    connected.set(false);
                    log.warn("MCP服务器连接意外断开: {}", config.getName());
                }
            }
        }, "MCP-Reader-" + config.getName());

        readerThread.setDaemon(true);
        readerThread.start();
    }
}
