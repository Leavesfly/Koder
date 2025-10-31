package io.leavesfly.koder.core.llm;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LLM提供商注册中心
 * 负责管理和路由不同的LLM提供商
 */
@Slf4j
public class LLMProviderRegistry {

    private final List<LLMProvider> providers = new ArrayList<>();

    /**
     * 注册提供商
     */
    public void registerProvider(LLMProvider provider) {
        providers.add(provider);
        log.info("注册LLM提供商: {}", provider.getProviderName());
    }

    /**
     * 根据模型名称查找提供商
     */
    public Optional<LLMProvider> findProvider(String modelName) {
        return providers.stream()
            .filter(p -> p.supports(modelName))
            .findFirst();
    }

    /**
     * 调用LLM
     */
    public Mono<LLMProvider.LLMResponse> call(String modelName,
                                               List<Map<String, Object>> messages,
                                               List<Map<String, Object>> tools) {
        return findProvider(modelName)
            .map(provider -> {
                log.debug("使用提供商: {} 调用模型: {}", provider.getProviderName(), modelName);
                return provider.call(messages, tools, modelName);
            })
            .orElseGet(() -> {
                log.error("未找到支持模型的提供商: {}", modelName);
                return Mono.error(new UnsupportedOperationException(
                    "不支持的模型: " + modelName));
            });
    }

    /**
     * 获取所有已注册的提供商
     */
    public List<LLMProvider> getAllProviders() {
        return new ArrayList<>(providers);
    }

    /**
     * 获取提供商数量
     */
    public int getProviderCount() {
        return providers.size();
    }
}
