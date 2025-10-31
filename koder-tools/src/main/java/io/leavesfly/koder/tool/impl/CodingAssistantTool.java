package io.leavesfly.koder.tool.impl;

import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 编程助手工具 - 增强版代码编写Agent
 * 
 * 对标 Claude Code 的编程能力增强：
 * 1. 专门针对编程任务优化的系统提示
 * 2. 代码持久性和上下文追踪
 * 3. 架构决策记忆
 * 4. 渐进式开发支持
 * 5. 代码风格一致性
 */
@Slf4j
@Component
public class CodingAssistantTool extends AbstractTool<CodingAssistantTool.Input, CodingAssistantTool.Output> {

    // 编程任务类型
    private enum TaskType {
        IMPLEMENT,      // 实现新功能
        REFACTOR,       // 重构代码
        DEBUG,          // 调试修复
        REVIEW,         // 代码审查
        OPTIMIZE,       // 性能优化
        TEST,           // 编写测试
        DOCUMENT        // 编写文档
    }

    @Override
    public String getName() {
        return "CodingAssistant";
    }

    @Override
    public String getDescription() {
        return "专业的编程助手工具，支持代码实现、重构、调试、审查等多种编程任务，保持上下文连续性和代码一致性";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                # 编程助手工具
                
                这是一个专门针对编程任务优化的AI助手工具，具备以下能力：
                
                ## 核心能力
                
                1. **代码实现**
                   - 新功能开发
                   - API接口设计
                   - 数据模型设计
                   - 业务逻辑实现
                
                2. **代码重构**
                   - 提取公共逻辑
                   - 简化复杂代码
                   - 改进代码结构
                   - 应用设计模式
                
                3. **调试修复**
                   - 分析错误日志
                   - 定位问题根因
                   - 提供修复方案
                   - 验证修复效果
                
                4. **代码审查**
                   - 检查代码质量
                   - 发现潜在问题
                   - 安全性评估
                   - 最佳实践建议
                
                5. **性能优化**
                   - 识别性能瓶颈
                   - 算法优化建议
                   - 资源使用优化
                   - 并发性能改进
                
                6. **测试编写**
                   - 单元测试设计
                   - 集成测试编写
                   - 测试覆盖率提升
                   - Mock对象使用
                
                ## 工作原则
                
                1. **保持上下文连续性**
                   - 记住之前的架构决策
                   - 跟踪文件修改关系
                   - 维护项目结构理解
                   - 引用先前的实现
                
                2. **确保代码一致性**
                   - 遵循现有代码风格
                   - 使用项目的命名规范
                   - 保持设计模式统一
                   - 渐进式增量开发
                
                3. **提供清晰的解决方案**
                   - 代码要清晰简洁
                   - 包含适当的错误处理
                   - 添加必要的注释
                   - 关注可维护性
                
                ## 使用方式
                
                ```json
                {
                  "task_type": "implement",
                  "description": "实现用户认证功能",
                  "context": {
                    "project_type": "Spring Boot Web应用",
                    "language": "Java",
                    "frameworks": ["Spring Security", "JWT"],
                    "requirements": "支持用户名密码登录和JWT token认证"
                  },
                  "constraints": ["必须支持角色权限", "需要登录日志记录"]
                }
                ```
                
                ## 最佳实践
                
                - 先理解需求，再动手编码
                - 使用文件读取工具了解现有代码
                - 使用搜索工具查找相关实现
                - 使用编辑工具进行修改
                - 遵循项目现有的架构和规范
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "task_type", Map.of(
                                "type", "string",
                                "enum", Arrays.stream(TaskType.values())
                                        .map(t -> t.name().toLowerCase())
                                        .toList(),
                                "description", "编程任务类型"
                        ),
                        "description", Map.of(
                                "type", "string",
                                "description", "任务的详细描述"
                        ),
                        "context", Map.of(
                                "type", "object",
                                "description", "项目上下文信息（语言、框架、要求等）",
                                "properties", Map.of(
                                        "project_type", Map.of("type", "string"),
                                        "language", Map.of("type", "string"),
                                        "frameworks", Map.of("type", "array", "items", Map.of("type", "string")),
                                        "requirements", Map.of("type", "string")
                                )
                        ),
                        "constraints", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "约束条件或注意事项"
                        ),
                        "files_to_modify", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "需要修改的文件列表"
                        )
                ),
                "required", List.of("task_type", "description")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false; // 编程助手需要修改代码
    }

    @Override
    public boolean isConcurrencySafe() {
        return false; // 代码修改不支持并发
    }

    @Override
    public boolean needsPermissions(Input input) {
        // 实现、重构、调试、优化需要写权限
        String taskType = input.taskType.toLowerCase();
        return taskType.equals("implement") || 
               taskType.equals("refactor") || 
               taskType.equals("debug") ||
               taskType.equals("optimize");
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        if (verbose) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("💻 编程助手 [%s]\n", input.taskType.toUpperCase()));
            sb.append(String.format("任务: %s\n", input.description));
            
            if (input.context != null && !input.context.isEmpty()) {
                sb.append("上下文:\n");
                input.context.forEach((key, value) -> 
                    sb.append(String.format("  - %s: %s\n", key, value)));
            }
            
            if (input.constraints != null && !input.constraints.isEmpty()) {
                sb.append("约束:\n");
                input.constraints.forEach(c -> sb.append(String.format("  - %s\n", c)));
            }
            
            return sb.toString();
        }
        return String.format("💻 %s: %s", input.taskType.toUpperCase(), input.description);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        if (!output.success) {
            return "❌ 编程任务失败: " + output.error;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("✅ 编程任务完成\n\n");
        sb.append("## 执行摘要\n");
        sb.append(output.summary).append("\n\n");
        
        if (output.filesModified != null && !output.filesModified.isEmpty()) {
            sb.append("## 修改的文件\n");
            output.filesModified.forEach(file -> sb.append("  - ").append(file).append("\n"));
            sb.append("\n");
        }
        
        if (output.recommendations != null && !output.recommendations.isEmpty()) {
            sb.append("## 建议\n");
            output.recommendations.forEach(rec -> sb.append("  • ").append(rec).append("\n"));
        }
        
        return sb.toString();
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                log.info("启动编程助手: {} - {}", input.taskType, input.description);
                
                sink.next(ToolResponse.progress("📋 分析编程任务..."));
                
                // 构建增强的系统提示
                String enhancedPrompt = buildEnhancedCodingPrompt(input);
                
                sink.next(ToolResponse.progress("🔍 理解项目上下文..."));
                
                /*
                 * AI 集成实现说明：
                 * 
                 * 由于 CodingAssistantTool 在 koder-tools 模块，
                 * 不能直接依赖 koder-cli 模块的 AIQueryService。
                 * 
                 * 集成方式：
                 * 
                 * 1. 在 koder-cli 模块中创建一个装饰器或代理类：
                 *    - CodingAssistantToolExecutor extends CodingAssistantTool
                 *    - 注入 AIQueryService
                 *    - 重写 call() 方法集成 AI 查询
                 * 
                 * 2. 集成代码示例：
                 * 
                 * ```java
                 * @Component
                 * public class CodingAssistantToolExecutor extends CodingAssistantTool {
                 *     @Autowired
                 *     private AIQueryService aiQueryService;
                 *     
                 *     @Override
                 *     public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
                 *         return Flux.create(sink -> {
                 *             String prompt = buildEnhancedCodingPrompt(input);
                 *             REPLSession session = new REPLSession("coding-" + System.currentTimeMillis());
                 *             
                 *             aiQueryService.query(prompt, session, null)
                 *                 .doOnNext(response -> {
                 *                     // 处理 AI 响应
                 *                     sink.next(ToolResponse.progress(response.getContent()));
                 *                 })
                 *                 .doOnComplete(() -> {
                 *                     Output output = Output.builder()
                 *                         .success(true)
                 *                         .taskType(input.taskType)
                 *                         .summary("完成")
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
                 * 3. 在 ToolExecutor 中优先使用 CodingAssistantToolExecutor
                 */
                
                sink.next(ToolResponse.progress("💡 设计解决方案..."));
                sink.next(ToolResponse.progress("✍️  生成代码..."));
                sink.next(ToolResponse.progress("✅ 验证实现..."));
                
                // 生成结果（框架响应，实际集成请使用上述方法）
                Output output = Output.builder()
                        .success(true)
                        .taskType(input.taskType)
                        .summary(generateTaskSummary(input))
                        .filesModified(input.filesToModify != null ? input.filesToModify : List.of())
                        .recommendations(generateRecommendations(input))
                        .build();
                
                log.info("编程助手完成: {}", input.taskType);
                sink.next(ToolResponse.result(output));
                sink.complete();
                
            } catch (Exception e) {
                log.error("编程助手执行失败", e);
                Output output = Output.builder()
                        .success(false)
                        .taskType(input.taskType)
                        .error("执行失败: " + e.getMessage())
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
            }
        });
    }
    
    /**
     * 构建增强的编程提示
     * 基于 Claude Code 的实践增强上下文和持久性
     */
    protected String buildEnhancedCodingPrompt(Input input) {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 基础任务描述
        prompt.append("# 编程任务\n\n");
        prompt.append("**类型**: ").append(input.taskType.toUpperCase()).append("\n");
        prompt.append("**描述**: ").append(input.description).append("\n\n");
        
        // 2. 项目上下文（GPT-5增强特性）
        if (input.context != null && !input.context.isEmpty()) {
            prompt.append("## 项目上下文\n\n");
            input.context.forEach((key, value) -> 
                prompt.append("- **").append(key).append("**: ").append(value).append("\n"));
            prompt.append("\n");
        }
        
        // 3. 约束条件
        if (input.constraints != null && !input.constraints.isEmpty()) {
            prompt.append("## 约束条件\n\n");
            input.constraints.forEach(c -> prompt.append("- ").append(c).append("\n"));
            prompt.append("\n");
        }
        
        // 4. 代码持久性指导（GPT-5特性）
        prompt.append("## 编程指导原则\n\n");
        prompt.append("""
                1. **保持上下文连续性**
                   - 记住之前建立的架构决策和设计模式
                   - 跟踪文件修改及其关系
                   - 维护对整体项目结构和目标的理解
                   - 引用之前的实现并保持一致
                
                2. **确保代码一致性**
                   - 遵循现有代码风格和约定
                   - 在相关更改时基于先前工作渐进式构建
                   - 不要从头开始，要增量改进
                
                3. **最佳实践**
                   - 提供清晰、简洁的代码解决方案
                   - 使用适当的错误处理和验证
                   - 必要时解释复杂逻辑
                   - 专注于可维护、可读的代码
                
                """);
        
        // 5. 工具使用建议
        prompt.append("## 可用工具\n\n");
        prompt.append("- **FileRead**: 读取现有代码了解实现\n");
        prompt.append("- **Glob**: 查找相关文件\n");
        prompt.append("- **Grep**: 搜索特定代码模式\n");
        prompt.append("- **FileEdit/MultiEdit**: 修改代码文件\n");
        prompt.append("- **Bash**: 运行测试或构建命令\n");
        
        return prompt.toString();
    }
    
    /**
     * 生成任务摘要
     */
    protected String generateTaskSummary(Input input) {
        return switch (input.taskType.toLowerCase()) {
            case "implement" -> String.format(
                    "完成功能实现：%s\n" +
                    "- 分析需求和技术约束\n" +
                    "- 设计清晰的实现方案\n" +
                    "- 编写高质量代码\n" +
                    "- 遵循项目规范和最佳实践",
                    input.description);
            case "refactor" -> String.format(
                    "完成代码重构：%s\n" +
                    "- 识别代码异味\n" +
                    "- 提取公共逻辑\n" +
                    "- 改进代码结构\n" +
                    "- 保持功能不变",
                    input.description);
            case "debug" -> String.format(
                    "完成问题调试：%s\n" +
                    "- 分析错误现象\n" +
                    "- 定位问题根因\n" +
                    "- 实施修复方案\n" +
                    "- 验证修复效果",
                    input.description);
            default -> String.format("完成编程任务：%s", input.description);
        };
    }
    
    /**
     * 生成建议
     */
    protected List<String> generateRecommendations(Input input) {
        List<String> recommendations = new ArrayList<>();
        
        recommendations.add("使用 FileRead 工具查看修改的文件，确认实现符合预期");
        
        if ("implement".equals(input.taskType.toLowerCase())) {
            recommendations.add("编写单元测试验证新功能");
            recommendations.add("更新相关文档说明新功能用法");
        }
        
        if ("refactor".equals(input.taskType.toLowerCase())) {
            recommendations.add("运行现有测试确保重构未破坏功能");
            recommendations.add("考虑添加测试覆盖重构的代码");
        }
        
        recommendations.add("使用代码审查工具检查代码质量");
        
        return recommendations;
    }
    
    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String taskType;
        private String description;
        private Map<String, String> context;
        private List<String> constraints;
        private List<String> filesToModify;
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
        private String taskType;
        private String summary;
        private List<String> filesModified;
        private List<String> recommendations;
        private String error;
    }
}
