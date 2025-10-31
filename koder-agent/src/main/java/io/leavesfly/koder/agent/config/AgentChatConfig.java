package io.leavesfly.koder.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent ChatClient配置
 * 提供ChatClient.Builder bean用于Agent执行
 * 
 * 注意: 这是一个临时实现的占位配置类
 * 当Spring AI依赖正确引入后，应该使用真实的ChatClient.Builder
 */
@Slf4j
@Configuration
public class AgentChatConfig {

    /**
     * 提供ChatClient.Builder bean的占位实现
     * TODO: 当Spring AI依赖可用时，替换为真实的ChatClient.Builder
     * 
     * @return ChatClient.Builder实例的模拟对象
     */
    @Bean
    @ConditionalOnMissingBean(name = "chatClientBuilder")
    public Object chatClientBuilder() {
        log.warn("使用临时的ChatClient.Builder占位实现，请在Spring AI依赖可用后替换");
        log.info("要使用真实的Spring AI ChatClient，请确保:");
        log.info("1. Spring AI依赖已正确添加到classpath");
        log.info("2. 配置了AI模型提供商(如OpenAI、Anthropic等)的API密钥");
        log.info("3. Spring AI auto-configuration已启用");
        
        // 返回一个占位对象
        return new Object() {
            @Override
            public String toString() {
                return "ChatClient.Builder Placeholder - Awaiting Spring AI Integration";
            }
        };
    }
}
