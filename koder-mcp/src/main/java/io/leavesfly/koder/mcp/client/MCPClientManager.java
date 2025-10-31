package io.leavesfly.koder.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.core.config.GlobalConfig;
import io.leavesfly.koder.mcp.client.transport.SSEMCPClient;
import io.leavesfly.koder.mcp.client.transport.StdioMCPClient;
import io.leavesfly.koder.mcp.config.MCPConfigConverter;
import io.leavesfly.koder.mcp.config.MCPServerConfig;
import io.leavesfly.koder.mcp.protocol.MCPTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP客户端管理器
 * 管理多个MCP服务器连接
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MCPClientManager {

    private final ConfigManager configManager;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    // 客户端缓存
    private final Map<String, MCPClient> clients = new ConcurrentHashMap<>();
    
    /**
     * 初始化MCP客户端管理器
     */
    public void initialize() {
        log.info("初始化MCP客户端管理器...");
        
        try {
            // 预加载所有配置的服务器
            List<MCPServerConfig> configs = getAllServerConfigsInternal();
            log.info("发现 {} 个MCP服务器配置", configs.size());
            
            // 可选：预连接服务器
            // configs.forEach(config -> {
            //     getClient(config.getName()).subscribe(
            //         client -> log.info("MCP服务器已连接: {}", config.getName()),
            //         error -> log.warn("MCP服务器连接失败: {} - {}", config.getName(), error.getMessage())
            //     );
            // });
        } catch (Exception e) {
            log.warn("MCP客户端管理器初始化失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取所有已连接的客户端
     */
    public Map<String, MCPClient> getAllClients() {
        return new ConcurrentHashMap<>(clients);
    }

    /**
     * 获取或创建MCP客户端
     */
    public Mono<MCPClient> getClient(String serverName) {
        // 检查缓存
        MCPClient cachedClient = clients.get(serverName);
        if (cachedClient != null && cachedClient.isConnected()) {
            return Mono.just(cachedClient);
        }

        // 创建新客户端
        return createClient(serverName)
                .flatMap(client -> client.connect()
                        .then(Mono.fromRunnable(() -> clients.put(serverName, client)))
                        .thenReturn(client)
                );
    }

    /**
     * 创建MCP客户端
     */
    private Mono<MCPClient> createClient(String serverName) {
        return Mono.fromCallable(() -> {
            // 从配置中查找服务器
            MCPServerConfig config = findServerConfig(serverName);
            if (config == null) {
                throw new RuntimeException("未找到MCP服务器配置: " + serverName);
            }

            // 根据类型创建客户端
            return switch (config.getType()) {
                case "stdio" -> new StdioMCPClient(config, objectMapper);
                case "sse" -> new SSEMCPClient(config, objectMapper, webClientBuilder);
                default -> throw new RuntimeException("不支持的传输类型: " + config.getType());
            };
        });
    }

    /**
     * 列出所有MCP服务器的工具
     */
    public Mono<List<MCPTool>> listAllTools() {
        return Mono.fromCallable(() -> {
            List<MCPServerConfig> configs = getAllServerConfigsInternal();
            return configs;
        }).flatMapMany(reactor.core.publisher.Flux::fromIterable)
          .flatMap(config -> 
              getClient(config.getName())
                      .flatMap(MCPClient::listTools)
                      .onErrorResume(error -> {
                          log.error("列出MCP服务器工具失败: {}", config.getName(), error);
                          return Mono.empty();
                      })
          )
          .collectList()
          .map(lists -> lists.stream()
                  .flatMap(List::stream)
                  .toList()
          );
    }

    /**
     * 断开所有客户端
     */
    public Mono<Void> disconnectAll() {
        return reactor.core.publisher.Flux.fromIterable(clients.values())
                .flatMap(MCPClient::disconnect)
                .then(Mono.fromRunnable(clients::clear));
    }

    /**
     * 从配置中查找服务器
     */
    private MCPServerConfig findServerConfig(String serverName) {
        try {
            GlobalConfig globalConfig = configManager.getGlobalConfig();
            if (globalConfig == null || globalConfig.getMcpServers() == null) {
                log.warn("MCP服务器配置为空");
                return null;
            }
            
            io.leavesfly.koder.core.config.McpServerConfig coreConfig = 
                    globalConfig.getMcpServers().get(serverName);
            
            if (coreConfig == null) {
                log.warn("MCP服务器配置未找到: {}", serverName);
                return null;
            }
            
            return MCPConfigConverter.convert(serverName, coreConfig);
        } catch (Exception e) {
            log.error("MCP服务器配置查找失败: {}", serverName, e);
            return null;
        }
    }

    /**
     * 内部方法 - 获取所有服务器配置
     */
    private List<MCPServerConfig> getAllServerConfigsInternal() {
        try {
            GlobalConfig globalConfig = configManager.getGlobalConfig();
            if (globalConfig == null || globalConfig.getMcpServers() == null) {
                log.debug("MCP服务器配置为空");
                return List.of();
            }
            
            return globalConfig.getMcpServers().entrySet().stream()
                    .map(entry -> MCPConfigConverter.convert(entry.getKey(), entry.getValue()))
                    .filter(config -> config != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("MCP服务器配置列表获取失败", e);
            return List.of();
        }
    }

    /**
     * 获取所有服务器配置
     */
    public List<MCPServerConfig> getAllServerConfigs() {
        return getAllServerConfigsInternal();
    }
}
