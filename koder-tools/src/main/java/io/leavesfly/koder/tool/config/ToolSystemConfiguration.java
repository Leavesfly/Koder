package io.leavesfly.koder.tool.config;

import io.leavesfly.koder.tool.Tool;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 工具系统配置
 * 自动注册所有Tool实现类到ToolExecutor
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ToolSystemConfiguration {

    /**
     * 创建ToolExecutor Bean
     * 自动注入所有Tool实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolExecutor toolExecutor(List<Tool<?, ?>> tools) {
        log.info("创建ToolExecutor，发现 {} 个工具", tools.size());
        
        ToolExecutor executor = new ToolExecutor();
        
        // 注册所有工具
        for (Tool<?, ?> tool : tools) {
            try {
                executor.registerTool(tool);
                log.debug("注册工具: {}", tool.getName());
            } catch (Exception e) {
                log.error("注册工具失败: {} - {}", tool.getName(), e.getMessage());
            }
        }
        
        return executor;
    }
}
