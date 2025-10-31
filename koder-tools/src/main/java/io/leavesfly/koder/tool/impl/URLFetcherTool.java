package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * URL获取工具
 * 从指定URL获取内容
 */
@Slf4j
@Component
public class URLFetcherTool extends AbstractTool<URLFetcherTool.Input, URLFetcherTool.Output> {

    private static final int MAX_CONTENT_LENGTH = 100 * 1024; // 100KB
    private static final int TIMEOUT_MS = 10000; // 10秒

    @Override
    public String getName() {
        return "FetchURL";
    }

    @Override
    public String getDescription() {
        return "从URL获取网页内容。支持HTTP和HTTPS协议。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return "使用此工具从URL获取网页内容。仅支持公开可访问的URL。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("url", "要获取的URL地址（必须是http或https）")
                .required("url")
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
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        if (input.url == null || input.url.isEmpty()) {
            return ValidationResult.failure("URL不能为空");
        }

        if (!input.url.startsWith("http://") && !input.url.startsWith("https://")) {
            return ValidationResult.failure("URL必须以http://或https://开头");
        }

        return ValidationResult.success();
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return String.format("url: %s", input.url);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        return String.format("获取成功: %s (%d 字节, 状态码: %d)",
                output.url, output.content.length(), output.statusCode);
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                URL url = new URL(input.url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setRequestProperty("User-Agent",
                        "Koder-Bot/1.0 (Java HTTP Client)");

                int statusCode = connection.getResponseCode();
                StringBuilder content = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                statusCode >= 400 ?
                                        connection.getErrorStream() :
                                        connection.getInputStream()))) {

                    String line;
                    int totalLength = 0;

                    while ((line = reader.readLine()) != null) {
                        totalLength += line.length();
                        if (totalLength > MAX_CONTENT_LENGTH) {
                            content.append("\n... (内容被截断，超过最大长度)");
                            break;
                        }
                        content.append(line).append("\n");
                    }
                }

                Output output = Output.builder()
                        .url(input.url)
                        .statusCode(statusCode)
                        .content(content.toString())
                        .contentType(connection.getContentType())
                        .success(statusCode >= 200 && statusCode < 300)
                        .build();

                sink.next(ToolResponse.result(output));
                sink.complete();

                log.info("URL获取完成: {}, 状态码: {}", input.url, statusCode);

            } catch (Exception e) {
                log.error("URL获取失败: {}", input.url, e);
                sink.error(new RuntimeException("URL获取失败: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String url;
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private String url;
        private int statusCode;
        private String content;
        private String contentType;
        private boolean success;
    }
}
