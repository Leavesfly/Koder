package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 网络搜索工具
 * 模拟搜索引擎功能（实际应用需要集成真实搜索API）
 */
@Slf4j
@Component
public class WebSearchTool extends AbstractTool<WebSearchTool.Input, WebSearchTool.Output> {

    @Override
    public String getName() {
        return "WebSearch";
    }

    @Override
    public String getDescription() {
        return "在网络上搜索信息。返回相关的搜索结果。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                使用此工具在网络上搜索信息：
                - query: 搜索查询（必需）
                - max_results: 最大结果数（可选，默认5）
                
                注意：当前为模拟实现，实际应用需要集成搜索API。
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("query", "搜索查询")
                .addNumberProperty("max_results", "最大结果数（默认5）")
                .required("query")
                .build();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    @Override
    public boolean needsPermissions(Input input) {
        return false;
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("query: %s", input.query);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个结果:\n", output.results.size()));

        for (int i = 0; i < Math.min(output.results.size(), 3); i++) {
            SearchResult result = output.results.get(i);
            sb.append(String.format("  %d. %s\n     %s\n",
                    i + 1, result.title, result.url));
        }

        if (output.results.size() > 3) {
            sb.append(String.format("  ... 还有 %d 个结果", output.results.size() - 3));
        }

        return sb.toString();
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                // 集成实际的搜索API
                // 
                // 可选的搜索API集成方案:
                //
                // 1. Google Custom Search API
                //    - 需要: Google API Key + Search Engine ID
                //    - 配置: application.properties 或环境变量
                //    - 示例: 
                //      String apiKey = config.getGoogleApiKey();
                //      String searchEngineId = config.getGoogleSearchEngineId();
                //      String url = String.format(
                //          "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s",
                //          apiKey, searchEngineId, URLEncoder.encode(input.query, "UTF-8")
                //      );
                //
                // 2. Bing Search API
                //    - 需要: Azure Cognitive Services API Key
                //    - 示例:
                //      WebClient client = WebClient.builder()
                //          .baseUrl("https://api.bing.microsoft.com/v7.0/search")
                //          .defaultHeader("Ocp-Apim-Subscription-Key", apiKey)
                //          .build();
                //
                // 3. DuckDuckGo Instant Answer API
                //    - 免费，无需API Key
                //    - URL: https://api.duckduckgo.com/?q={query}&format=json
                //
                // 4. SerpAPI
                //    - 封装多种搜索引擎的统一API
                //    - 支持 Google, Bing, Baidu 等
                //
                // 实现建议:
                // - 使用 WebClient 进行 HTTP 请求
                // - 配置超时和重试机制
                // - 缓存搜索结果（避免API限制）
                // - 解析JSON响应并提取标题、URL、摘要
                //
                // 目前使用模拟数据，待集成搜索API后替换
                
                int maxResults = input.maxResults != null ? input.maxResults : 5;
                List<SearchResult> results = generateMockResults(input.query, maxResults);

                Output output = Output.builder()
                        .query(input.query)
                        .results(results)
                        .totalResults(results.size())
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.info("网络搜索完成: query={}, results={}", input.query, results.size());

            } catch (Exception e) {
                log.error("网络搜索失败: {}", input.query, e);
                sink.error(new RuntimeException("网络搜索失败: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 生成模拟搜索结果
     * 此方法将在集成搜索API后被替换
     */
    private List<SearchResult> generateMockResults(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < Math.min(maxResults, 5); i++) {
            SearchResult result = SearchResult.builder()
                    .title(String.format("搜索结果 %d: %s", i + 1, query))
                    .url(String.format("https://example.com/result%d", i + 1))
                    .snippet(String.format("这是关于 '%s' 的搜索结果摘要 %d。" +
                            "包含相关信息和描述...", query, i + 1))
                    .build();
            results.add(result);
        }
        
        // 添加模拟标记
        if (!results.isEmpty()) {
            SearchResult firstResult = results.get(0);
            firstResult.setSnippet(firstResult.getSnippet() + 
                    "\n\n⚠️ 注意: 这是模拟数据。集成搜索API后将返回真实结果。");
        }

        return results;
    }

    /**
     * 搜索结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String title;
        private String url;
        private String snippet;
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String query;
        private Integer maxResults;
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private String query;
        private List<SearchResult> results;
        private int totalResults;
    }
}
