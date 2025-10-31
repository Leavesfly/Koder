package io.leavesfly.koder.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.mcp.client.MCPClientManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MCP系统配置
 * 配置MCP相关的Spring Bean
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MCPSystemConfiguration {

    /**
     * 创建MCPClientManager Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MCPClientManager mcpClientManager(
            ConfigManager configManager,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder) {
        log.debug("创建MCPClientManager");
        return new MCPClientManager(configManager, objectMapper, webClientBuilder);
    }
}
