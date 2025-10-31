package io.leavesfly.koder.agent.config;

import io.leavesfly.koder.agent.AgentRegistry;
import io.leavesfly.koder.agent.executor.AgentExecutor;
import io.leavesfly.koder.agent.loader.AgentLoader;
//import io.leavesfly.koder.core.ai.LlmManager;
import io.leavesfly.koder.core.config.ConfigManager;
import io.leavesfly.koder.tool.executor.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent系统配置
 * 配置Agent相关的Spring Bean
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentSystemConfiguration {

    /**
     * 创建AgentLoader Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentLoader agentLoader() {
        log.debug("创建AgentLoader");
        return new AgentLoader();
    }

    /**
     * 创建AgentRegistry Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentRegistry agentRegistry(AgentLoader agentLoader) {
        log.debug("创建AgentRegistry");
        return new AgentRegistry(agentLoader);
    }

//    /**
//     * 创建AgentExecutor Bean
//     */
//    @Bean
//    @ConditionalOnMissingBean
//    public AgentExecutor agentExecutor(
//            AgentRegistry agentRegistry,
//            ToolExecutor toolExecutor,
//            LlmManager llmManager) {
//        log.debug("创建AgentExecutor");
//        return new AgentExecutor(agentRegistry, toolExecutor,llmManager);
//    }
}
