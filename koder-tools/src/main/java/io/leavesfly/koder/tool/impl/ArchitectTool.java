package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 架构师工具 - 用于技术分析和架构设计
 * 
 * 这是一个高级工具,可以调用其他探索性工具（如 FileRead、Bash、Glob 等）
 * 来分析代码库并提供架构建议
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchitectTool extends AbstractTool<ArchitectTool.Input, ArchitectTool.Output> {

    @Override
    public String getName() {
        return "Architect";
    }

    @Override
    public String getDescription() {
        return "技术架构分析和设计工具，可以调用其他工具深入分析代码库";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                架构师工具 - 技术分析与架构设计
                
                用途：
                - 分析现有代码库的架构
                - 设计新功能的技术方案
                - 评估技术选型
                - 提供最佳实践建议
                - 识别潜在的架构问题
                
                工作方式：
                - 接收技术需求或问题描述
                - 可以调用文件系统探索工具（FileRead、Bash、Glob、Grep、LS）
                - 分析代码结构和依赖关系
                - 生成详细的架构分析报告
                
                适用场景：
                - 新功能技术设计
                - 重构方案制定
                - 性能优化分析
                - 安全性评估
                - 技术债务识别
                
                注意：
                - 这是一个只读分析工具
                - 不会修改任何文件
                - 专注于技术架构层面
                - 可能需要较长的分析时间
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "prompt", Map.of(
                                "type", "string",
                                "description", "技术需求或要分析的问题"
                        ),
                        "context", Map.of(
                                "type", "string",
                                "description", "可选的上下文信息或背景说明"
                        )
                ),
                "required", List.of("prompt")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true; // 架构工具是只读的
    }

    @Override
    public boolean isConcurrencySafe() {
        return true; // 只读操作，可以并发
    }

    @Override
    public boolean needsPermissions(Input input) {
        return false; // 只读分析不需要特殊权限
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        if (verbose) {
            String preview = input.prompt.length() > 100 
                    ? input.prompt.substring(0, 100) + "..." 
                    : input.prompt;
            return String.format("架构分析: %s%s",
                    preview,
                    input.context != null ? "\n上下文: " + input.context : "");
        }
        return "进行架构分析...";
    }

    @Override
    public String renderToolResultMessage(Output output) {
        if (!output.success) {
            return "❌ 分析失败: " + output.error;
        }
        return output.analysis;
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                // 构建架构分析提示
                String analysisPrompt = buildAnalysisPrompt(input);
                
                sink.next(ToolResponse.progress("🏗️ 开始架构分析..."));
                
                // 注意：完整实现需要集成 AI 模型来执行分析
                // 这里提供一个框架实现，实际使用时需要调用 AIQueryService
                
                String architectSystemPrompt = """
                        你是一位资深的软件架构师，专长于：
                        - 系统架构设计与评估
                        - 技术选型与最佳实践
                        - 性能优化与可扩展性设计
                        - 安全性分析与风险评估
                        - 代码质量与维护性改进
                        
                        请根据用户的需求提供：
                        1. 清晰的技术分析
                        2. 具体的架构建议
                        3. 潜在风险和注意事项
                        4. 实施步骤或行动计划
                        
                        你可以使用以下工具来探索代码库：
                        - FileRead: 读取文件内容
                        - Bash: 执行命令（如 find, grep）
                        - Glob: 查找匹配模式的文件
                        - Grep: 搜索代码内容
                        - LS: 列出目录内容
                        
                        请基于实际代码分析给出建议，而不是泛泛而谈。
                        """;
                
                /*
                 * AI 集成实现说明：
                 * 
                 * 由于 ArchitectTool 在 koder-tools 模块，
                 * 不能直接依赖 koder-cli 模块的 AIQueryService。
                 * 
                 * 集成方式（与 CodingAssistantTool 相同）：
                 * 
                 * 1. 在 koder-cli 模块中创建装饰器类：
                 *    - ArchitectToolExecutor extends ArchitectTool
                 *    - 注入 AIQueryService 和 ToolExecutor
                 *    - 重写 call() 方法集成 AI 查询
                 * 
                 * 2. 集成代码示例：
                 * 
                 * ```java
                 * @Component
                 * public class ArchitectToolExecutor extends ArchitectTool {
                 *     @Autowired
                 *     private AIQueryService aiQueryService;
                 *     @Autowired
                 *     private ToolExecutor toolExecutor;
                 *     
                 *     @Override
                 *     public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
                 *         return Flux.create(sink -> {
                 *             String prompt = buildAnalysisPrompt(input);
                 *             REPLSession session = new REPLSession("architect-" + System.currentTimeMillis());
                 *             
                 *             // 准备系统提示
                 *             String systemPrompt = ARCHITECT_SYSTEM_PROMPT;
                 *             
                 *             // 限制可用工具为探索性工具
                 *             List<Tool<?, ?>> explorationTools = toolExecutor.getAllTools().stream()
                 *                 .filter(t -> List.of("FileRead", "Bash", "Glob", "Grep", "LS").contains(t.getName()))
                 *                 .toList();
                 *             
                 *             aiQueryService.query(prompt, session, systemPrompt)
                 *                 .doOnNext(response -> {
                 *                     if (response.getType() == AIResponseType.TEXT) {
                 *                         sink.next(ToolResponse.progress("📊 " + response.getContent()));
                 *                     }
                 *                 })
                 *                 .doOnComplete(() -> {
                 *                     // 提取分析结果
                 *                     String analysis = session.getMessages().stream()
                 *                         .filter(m -> m instanceof AssistantMessage)
                 *                         .map(m -> ((AssistantMessage) m).getContent())
                 *                         .collect(Collectors.joining("\n"));
                 *                     
                 *                     Output output = Output.builder()
                 *                         .success(true)
                 *                         .analysis(analysis)
                 *                         .prompt(input.prompt)
                 *                         .build();
                 *                     sink.next(ToolResponse.result(output));
                 *                     sink.complete();
                 *                 })
                 *                 .subscribe();
                 *         });
                 *     }
                 * }
                 * ```
                 * 
                 * 3. 在 ToolExecutor 中注册时优先使用 ArchitectToolExecutor
                 */
                
                // 当前使用框架响应（完整集成请使用上述方法）
                String analysis = generateAnalysisTemplate(input);
                
                sink.next(ToolResponse.progress("✅ 分析完成"));
                
                log.info("架构分析完成: {}", input.prompt);
                
                Output output = Output.builder()
                        .success(true)
                        .analysis(analysis)
                        .prompt(input.prompt)
                        .build();
                
                sink.next(ToolResponse.result(output));
                sink.complete();
                
            } catch (Exception e) {
                log.error("架构分析失败", e);
                Output output = Output.builder()
                        .success(false)
                        .error("分析失败: " + e.getMessage())
                        .prompt(input.prompt)
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
            }
        });
    }
    
    /**
     * 构建分析提示
     */
    private String buildAnalysisPrompt(Input input) {
        StringBuilder prompt = new StringBuilder();
        
        if (input.context != null && !input.context.isEmpty()) {
            prompt.append("<context>\n");
            prompt.append(input.context);
            prompt.append("\n</context>\n\n");
        }
        
        prompt.append(input.prompt);
        
        return prompt.toString();
    }
    
    /**
     * 生成分析模板
     * 注意：这是一个占位实现，实际应该通过 AI 模型生成
     */
    private String generateAnalysisTemplate(Input input) {
        return String.format("""
                # 架构分析报告
                
                ## 需求理解
                %s
                
                ## 技术分析
                
                为了提供准确的架构分析，建议：
                
                1. **代码库探索**
                   - 使用 Glob 工具查找相关文件
                   - 使用 FileRead 工具读取关键文件
                   - 使用 Grep 工具搜索特定模式
                
                2. **依赖分析**
                   - 检查项目配置文件（pom.xml, package.json 等）
                   - 分析导入和依赖关系
                   - 识别核心组件和模块
                
                3. **架构设计**
                   - 基于现有架构提供建议
                   - 考虑可扩展性和维护性
                   - 评估性能和安全性
                
                ## 建议
                
                需要实际的代码分析才能提供具体建议。
                
                请使用文件探索工具深入分析代码库，然后我将提供详细的架构建议。
                
                ## 注意事项
                
                - 这是 ArchitectTool 的框架实现
                - 完整功能需要集成 AIQueryService
                - 当前版本提供分析框架和指导
                
                ---
                
                💡 提示：完整的架构分析需要访问代码库并调用其他工具。
                建议先使用 FileRead、Glob 等工具收集信息。
                """, input.prompt);
    }
    
    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String prompt;
        private String context;
    }
    
    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private boolean success;
        private String analysis;
        private String prompt;
        private String error;
    }
}
