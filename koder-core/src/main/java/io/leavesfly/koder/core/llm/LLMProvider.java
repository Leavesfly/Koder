package io.leavesfly.koder.core.llm;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * LLM提供商接口
 * 定义与大语言模型交互的标准契约
 */
public interface LLMProvider {

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 调用LLM并返回完整响应
     * 
     * @param messages 消息列表
     * @param tools 工具定义列表
     * @param modelName 模型名称
     * @return LLM响应
     */
    Mono<LLMResponse> call(List<Map<String, Object>> messages, 
                           List<Map<String, Object>> tools, 
                           String modelName);

    /**
     * 检查是否支持指定模型
     */
    boolean supports(String modelName);

    /**
     * LLM响应类
     */
    class LLMResponse {
        private final String content;
        private final List<Map<String, Object>> toolCalls;

        public LLMResponse(String content, List<Map<String, Object>> toolCalls) {
            this.content = content;
            this.toolCalls = toolCalls;
        }

        public String getContent() {
            return content;
        }

        public List<Map<String, Object>> getToolCalls() {
            return toolCalls;
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
