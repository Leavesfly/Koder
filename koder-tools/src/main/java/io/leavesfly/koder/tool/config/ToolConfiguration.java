package io.leavesfly.koder.tool.config;

import io.leavesfly.koder.tool.Tool;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import io.leavesfly.koder.tool.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 工具系统配置
 * 负责自动注册所有工具到ToolExecutor
 */
@Slf4j
@Configuration
public class ToolConfiguration {

    /**
     * 初始化工具执行器并注册所有工具
     */
    @Bean
    public ToolExecutor toolExecutor(List<Tool<?, ?>> tools) {
        ToolExecutor executor = new ToolExecutor();

        // 注册所有通过Spring管理的工具
        executor.registerTools(tools);

        log.info("工具系统初始化完成，已注册 {} 个工具", tools.size());
        tools.forEach(tool -> log.info("  - {}: {}", tool.getName(), tool.getDescription()));

        return executor;
    }

    /**
     * 可选：提供工具列表Bean（用于调试和测试）
     */
    @Bean
    @ConditionalOnProperty(name = "koder.tools.list-enabled", havingValue = "true")
    public List<Tool<?, ?>> availableTools(
            FileReadTool fileReadTool,
            FileWriteTool fileWriteTool,
            FileEditTool fileEditTool,
            GlobTool globTool,
            GrepTool grepTool,
            LSTool lsTool,
            BashTool bashTool,
            ThinkTool thinkTool,
            TaskTool taskTool,
            AskExpertTool askExpertTool,
            URLFetcherTool urlFetcherTool,
            WebSearchTool webSearchTool,
            MemoryReadTool memoryReadTool,
            MemoryWriteTool memoryWriteTool) {

        return List.of(
                fileReadTool,
                fileWriteTool,
                fileEditTool,
                globTool,
                grepTool,
                lsTool,
                bashTool,
                thinkTool,
                taskTool,
                askExpertTool,
                urlFetcherTool,
                webSearchTool,
                memoryReadTool,
                memoryWriteTool
        );
    }
}
