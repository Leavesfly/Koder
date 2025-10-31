package io.leavesfly.koder.core.cost;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 成本追踪器
 * 追踪每个会话的Token使用和成本
 */
@Slf4j
@Data
public class CostTracker {
    
    /**
     * 会话ID
     */
    private final String sessionId;
    
    /**
     * 按模型分组的Token使用
     */
    private final Map<String, TokenUsage> usageByModel = new HashMap<>();
    
    /**
     * 总使用量
     */
    private final AtomicReference<TokenUsage> totalUsage = new AtomicReference<>(new TokenUsage());
    
    /**
     * 模型定价信息（每百万Token的价格）
     */
    private static final Map<String, ModelPricing> PRICING = new HashMap<>();
    
    static {
        // Anthropic Claude
        PRICING.put("claude-3-5-sonnet-20241022", new ModelPricing(3.0, 15.0));
        PRICING.put("claude-3-5-haiku-20241022", new ModelPricing(1.0, 5.0));
        PRICING.put("claude-3-opus-20240229", new ModelPricing(15.0, 75.0));
        
        // OpenAI GPT
        PRICING.put("gpt-4o", new ModelPricing(2.5, 10.0));
        PRICING.put("gpt-4o-mini", new ModelPricing(0.15, 0.6));
        PRICING.put("gpt-5", new ModelPricing(5.0, 20.0));
        PRICING.put("o1-preview", new ModelPricing(15.0, 60.0));
        PRICING.put("o1-mini", new ModelPricing(3.0, 12.0));
        
        // Google Gemini
        PRICING.put("gemini-2.0-flash-exp", new ModelPricing(0.0, 0.0)); // 免费
        PRICING.put("gemini-1.5-pro", new ModelPricing(1.25, 5.0));
        PRICING.put("gemini-1.5-flash", new ModelPricing(0.075, 0.3));
        
        // 国产模型
        PRICING.put("qwen-max", new ModelPricing(0.04, 0.12));
        PRICING.put("qwen-plus", new ModelPricing(0.008, 0.024));
        PRICING.put("qwen-turbo", new ModelPricing(0.003, 0.006));
        PRICING.put("deepseek-chat", new ModelPricing(0.001, 0.002));
        PRICING.put("deepseek-reasoner", new ModelPricing(0.55, 2.19));
    }
    
    public CostTracker(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * 记录Token使用
     */
    public void recordUsage(String modelName, TokenUsage usage) {
        if (usage == null) {
            return;
        }
        
        // 按模型统计
        usageByModel.compute(modelName, (k, v) -> {
            if (v == null) {
                v = new TokenUsage();
            }
            v.add(usage);
            return v;
        });
        
        // 更新总量
        totalUsage.updateAndGet(total -> {
            total.add(usage);
            return total;
        });
        
        log.debug("记录Token使用 - 模型: {}, 输入: {}, 输出: {}", 
                modelName, usage.getInputTokens(), usage.getOutputTokens());
    }
    
    /**
     * 计算总成本（美元）
     */
    public BigDecimal calculateTotalCost() {
        BigDecimal total = BigDecimal.ZERO;
        
        for (Map.Entry<String, TokenUsage> entry : usageByModel.entrySet()) {
            String modelName = entry.getKey();
            TokenUsage usage = entry.getValue();
            
            BigDecimal cost = calculateCost(modelName, usage);
            total = total.add(cost);
        }
        
        return total.setScale(6, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算特定模型的成本
     */
    private BigDecimal calculateCost(String modelName, TokenUsage usage) {
        ModelPricing pricing = PRICING.get(modelName);
        if (pricing == null) {
            // 未知模型，使用GPT-4o的价格估算
            pricing = PRICING.get("gpt-4o");
            log.warn("未知模型定价: {}, 使用默认定价", modelName);
        }
        
        // 成本 = (输入Token * 输入单价 + 输出Token * 输出单价) / 1,000,000
        BigDecimal inputCost = BigDecimal.valueOf(usage.getInputTokens())
                .multiply(BigDecimal.valueOf(pricing.inputPricePerMillion))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        
        BigDecimal outputCost = BigDecimal.valueOf(usage.getOutputTokens())
                .multiply(BigDecimal.valueOf(pricing.outputPricePerMillion))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        
        return inputCost.add(outputCost);
    }
    
    /**
     * 获取格式化的成本报告
     */
    public String formatCostReport() {
        StringBuilder sb = new StringBuilder();
        
        TokenUsage total = totalUsage.get();
        BigDecimal totalCost = calculateTotalCost();
        
        sb.append("总Token使用量: ").append(total.getTotalTokens()).append("\n");
        sb.append("  - 输入: ").append(total.getInputTokens()).append("\n");
        sb.append("  - 输出: ").append(total.getOutputTokens()).append("\n");
        
        if (total.getCacheReadTokens() > 0) {
            sb.append("  - 缓存读取: ").append(total.getCacheReadTokens()).append("\n");
        }
        if (total.getCacheCreationTokens() > 0) {
            sb.append("  - 缓存创建: ").append(total.getCacheCreationTokens()).append("\n");
        }
        
        sb.append("\n预估成本: $").append(totalCost).append("\n");
        
        if (usageByModel.size() > 1) {
            sb.append("\n按模型统计:\n");
            usageByModel.forEach((model, usage) -> {
                BigDecimal cost = calculateCost(model, usage);
                sb.append("  ").append(model).append(":\n");
                sb.append("    Token: ").append(usage.getTotalTokens());
                sb.append(" (输入: ").append(usage.getInputTokens());
                sb.append(", 输出: ").append(usage.getOutputTokens()).append(")\n");
                sb.append("    成本: $").append(cost).append("\n");
            });
        }
        
        return sb.toString();
    }
    
    /**
     * 模型定价信息
     */
    private record ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {}
}
