package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 思考工具
 * 允许AI记录其思考过程（用于支持思维链推理）
 */
@Slf4j
@Component
public class ThinkTool extends AbstractTool<ThinkTool.Input, ThinkTool.Output> {

    @Override
    public String getName() {
        return "Think";
    }

    @Override
    public String getDescription() {
        return "记录AI的思考过程。用于复杂推理和问题分析。";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return "使用此工具记录你的思考过程。这有助于进行更深入的分析和推理。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
                .addStringProperty("thought", "你的思考内容")
                .required("thought")
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
    public boolean isEnabled() {
        // 可以通过环境变量或配置控制是否启用
        return Boolean.parseBoolean(System.getProperty("koder.think.enabled", "true"));
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        return input.thought;
    }

    @Override
    public String renderToolResultMessage(Output output) {
        return "思考已记录";
    }

    @Override
    public Object renderResultForAssistant(Output output) {
        return "你的思考已被记录。";
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            log.debug("AI思考: {}", input.thought);

            Output output = Output.builder()
                    .thought(input.thought)
                    .messageId(context.getMessageId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            sink.next(ToolResponse.result(output));
            sink.complete();
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
        /**
         * 思考内容
         */
        private String thought;
    }

    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        /**
         * 思考内容
         */
        private String thought;

        /**
         * 消息ID
         */
        private String messageId;

        /**
         * 时间戳
         */
        private long timestamp;
    }
}
