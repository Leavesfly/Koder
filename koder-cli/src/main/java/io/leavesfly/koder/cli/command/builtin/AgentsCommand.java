package io.leavesfly.koder.cli.command.builtin;

import io.leavesfly.koder.agent.AgentConfig;
import io.leavesfly.koder.agent.AgentConfig.AgentLocation;
import io.leavesfly.koder.agent.AgentRegistry;
import io.leavesfly.koder.agent.executor.AgentExecutor;
import io.leavesfly.koder.cli.command.Command;
import io.leavesfly.koder.cli.command.CommandContext;
import io.leavesfly.koder.cli.command.CommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * agents命令增强版 - 全功能代理管理
 * 功能：
 * 1. 列出所有代理（按位置分组）
 * 2. 查看代理详情
 * 3. 创建新代理 (AI生成或手动)
 * 4. 编辑代理配置
 * 5. 删除代理
 * 6. 验证代理配置
 */
@Slf4j
@Component("agentsCommandEnhanced")
@RequiredArgsConstructor
public class AgentsCommand implements Command {

    private final AgentRegistry agentRegistry;

    private final AgentExecutor agentExecutor;

    private static final List<String> RESERVED_NAMES = List.of(
            "help", "exit", "quit", "agents", "task", "model", "config", "tools", "mcp"
    );

    private static final String CLAUDE_DIR = ".claude";
    private static final String AGENTS_DIR = "agents";

    @Override
    public String getName() {
        return "agents";
    }

    @Override
    public String getDescription() {
        return "全功能代理管理 - 创建、编辑、删除和查看代理";
    }

    @Override
    public String getUsage() {
        return "/agents [list|create|edit|delete|view|validate|run] [代理名称] [--ai]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String[] args = context.getArgs().toArray(new String[0]);

        // 无参数：列出所有代理
        if (args.length == 0) {
            return listAllAgents(context);
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "list", "ls" -> listAllAgents(context);
            case "create", "new" -> createAgent(context, args);
            case "edit", "update" -> editAgent(context, args);
            case "delete", "remove", "rm" -> deleteAgent(context, args);
            case "view", "show", "info" -> viewAgent(context, args);
            case "validate" -> validateAgent(context, args);
            case "run", "exec", "execute" -> runAgent(context, args);
            default -> {
                // 当做代理名称处理
                yield viewAgentByName(subCommand);
            }
        };
    }

    /**
     * 列出所有代理（按位置分组）
     */
    private CommandResult listAllAgents(CommandContext context) {
        List<AgentConfig> agents = agentRegistry.getAllAgents();

        if (agents.isEmpty()) {
            return CommandResult.success("\n未找到任何代理配置\n\n使用 /agents create 创建新代理\n");
        }

        StringBuilder output = new StringBuilder();
        output.append("\n=== 可用代理 ===\n");

        // 按位置分组
        Map<String, List<AgentConfig>> grouped = agents.stream()
                .collect(Collectors.groupingBy(a -> a.getLocation().getValue()));

        // 排序: built-in, user, project
        List<String> orderedLocations = List.of("built-in", "user", "project");
        for (String loc : orderedLocations) {
            if (grouped.containsKey(loc)) {
                output.append(String.format("\n📍 %s\n", loc.toUpperCase()));

                for (AgentConfig agent : grouped.get(loc)) {
                    output.append(String.format("  📦 %-20s", agent.getAgentType()));

                    // 工具权限简述
                    if (agent.allowsAllTools()) {
                        output.append(" [所有工具]");
                    } else {
                        output.append(String.format(" [%d个工具]", agent.getTools().size()));
                    }

                    // 模型覆盖
                    if (agent.getModelName() != null) {
                        output.append(String.format(" 🤖%s", agent.getModelName()));
                    }

                    output.append("\n");
                    output.append(String.format("     %s\n", truncate(agent.getWhenToUse(), 70)));
                }
            }
        }

        output.append(String.format("\n总计: %d 个代理\n\n", agents.size()));
        output.append("命令:\n");
        output.append("  /agents view <名称>    - 查看详情\n");
        output.append("  /agents create        - 创建新代理\n");
        output.append("  /agents create --ai   - AI生成代理\n");
        output.append("  /agents edit <名称>   - 编辑代理\n");
        output.append("  /agents delete <名称> - 删除代理\n");

        return CommandResult.success(output.toString());
    }

    /**
     * 查看代理详情
     */
    private CommandResult viewAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("请指定代理名称\n用法: /agents view <代理名称>");
        }

        return viewAgentByName(args[1]);
    }

    private CommandResult viewAgentByName(String agentType) {
        return agentRegistry.getAgentByType(agentType)
                .map(agent -> {
                    StringBuilder output = new StringBuilder();
                    output.append("\n╔══════════════════════════════════════════╗\n");
                    output.append(String.format("║  代理: %-32s ║\n", agent.getAgentType()));
                    output.append("╚══════════════════════════════════════════╝\n\n");

                    output.append(String.format("📍 位置: %s\n", agent.getLocation().getValue()));
                    output.append(String.format("📝 何时使用:\n   %s\n\n",
                            wrapText(agent.getWhenToUse(), 70, "   ")));

                    // 工具权限
                    output.append("🛠️  工具权限:\n");
                    if (agent.allowsAllTools()) {
                        output.append("   ✓ 所有工具 (*)\n");
                    } else {
                        List<String> tools = agent.getTools();
                        for (int i = 0; i < tools.size(); i++) {
                            output.append(String.format("   %d. %s\n", i + 1, tools.get(i)));
                        }
                    }

                    // 模型覆盖
                    if (agent.getModelName() != null) {
                        output.append(String.format("\n🤖 指定模型: %s\n", agent.getModelName()));
                    } else {
                        output.append("\n🤖 模型: 继承主模型\n");
                    }

                    // 颜色
                    if (agent.getColor() != null) {
                        output.append(String.format("🎨 UI颜色: %s\n", agent.getColor()));
                    }

                    // 系统提示词
                    output.append("\n💬 系统提示词:\n");
                    output.append("─".repeat(70) + "\n");
                    output.append(wrapText(agent.getSystemPrompt(), 70, ""));
                    output.append("\n" + "─".repeat(70) + "\n");

                    return CommandResult.success(output.toString());
                })
                .orElse(CommandResult.failure("未找到代理: " + agentType));
    }

    /**
     * 创建新代理
     */
    private CommandResult createAgent(CommandContext context, String[] args) {
        boolean useAI = List.of(args).contains("--ai");

        context.getOutput().println("\n=== 创建新代理 ===\n");

        if (useAI) {
            return createAgentWithAI(context);
        } else {
            return createAgentManual(context);
        }
    }

    /**
     * AI生成代理
     */
    private CommandResult createAgentWithAI(CommandContext context) {
        context.getOutput().println("使用AI生成代理配置\n");

        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            // 1. 获取代理的功能描述
            context.getOutput().println("请描述代理的功能和用途: ");
            System.out.print("> ");
            String description = scanner.nextLine().trim();

            if (description.isEmpty()) {
                return CommandResult.failure("描述不能为空");
            }

            // 2. 生成代理名称建议
            context.getOutput().println("\n代理名称 (kebab-case, 回车使用AI推荐): ");
            System.out.print("> ");
            String agentName = scanner.nextLine().trim();

            if (agentName.isEmpty()) {
                // AI推荐名称（简单实现）
                agentName = description.toLowerCase()
                        .replaceAll("[^a-z0-9\\s-]", "")
                        .replaceAll("\\s+", "-")
                        .replaceAll("-+", "-");
                context.getOutput().println("使用AI生成的名称: " + agentName);
            }

            // 3. 选择位置
            context.getOutput().println("\n保存位置 (user/project) [默认: user]: ");
            System.out.print("> ");
            String location = scanner.nextLine().trim();
            if (location.isEmpty()) {
                location = "user";
            }

            // 4. AI生成系统提示词（简化版）
            String systemPrompt = String.format(
                    "你是一个专业的%s代理。\n\n"
                            + "你的主要职责是: %s\n\n"
                            + "请始终保持专业、高效和严谨的工作态度。",
                    agentName, description
            );

            context.getOutput().println("\n生成的系统提示词:");
            context.getOutput().println(systemPrompt);

            // 5. 确认
            context.getOutput().println("\n是否创建此代理? (y/n): ");
            System.out.print("> ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (!confirm.equals("y") && !confirm.equals("yes")) {
                return CommandResult.success("已取消");
            }

            // 6. 生成代理文件
            String agentContent = buildAgentMarkdown(agentName, description, systemPrompt);

            // 7. 保存文件
            Path agentDir = location.equals("user")
                    ? Paths.get(System.getProperty("user.home"), ".claude", "agents")
                    : Paths.get(System.getProperty("user.dir"), ".claude", "agents");

            Files.createDirectories(agentDir);
            Path agentFile = agentDir.resolve(agentName + ".md");

            if (Files.exists(agentFile)) {
                context.getOutput().println("代理文件已存在，是否覆盖? (y/n): ");
                System.out.print("> ");
                String overwrite = scanner.nextLine().trim().toLowerCase();
                if (!overwrite.equals("y") && !overwrite.equals("yes")) {
                    return CommandResult.success("已取消");
                }
            }

            Files.writeString(agentFile, agentContent);

            // 刷新注册表
            agentRegistry.reload();

            return CommandResult.success(String.format(
                    "\n✅ 代理创建成功!\n\n"
                            + "名称: %s\n"
                            + "位置: %s\n"
                            + "文件: %s\n\n"
                            + "使用: /agents run %s <任务描述>\n",
                    agentName, location, agentFile, agentName
            ));

        } catch (Exception e) {
            log.error("创建代理失败", e);
            return CommandResult.failure("创建失败: " + e.getMessage());
        }
    }

    /**
     * 构建代理Markdown文件
     */
    private String buildAgentMarkdown(String name, String description, String systemPrompt) {
        return String.format("""
                # %s
                                
                ## 何时使用
                %s
                                
                ## 工具
                - *
                                
                ## 系统提示词
                %s
                """, name, description, systemPrompt);
    }

    /**
     * 手动创建代理
     */
    private CommandResult createAgentManual(CommandContext context) {
        context.getOutput().println("手动创建代理配置\n");

        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            // 1. 选择位置 (user/project)
            context.getOutput().println("步骤 1/6: 选择保存位置");
            context.getOutput().println("  - user: 用户级代理（所有项目可用）");
            context.getOutput().println("  - project: 项目级代理（仅当前项目可用）");
            System.out.print("> 请选择 (user/project) [默认: user]: ");
            String location = scanner.nextLine().trim();
            if (location.isEmpty()) {
                location = "user";
            }
            if (!location.equals("user") && !location.equals("project")) {
                return CommandResult.failure("无效的位置，请选择 user 或 project");
            }

            // 2. 输入代理名称
            context.getOutput().println("\n步骤 2/6: 输入代理名称");
            context.getOutput().println("要求: 使用 kebab-case 格式，例如: code-reviewer, bug-fixer");
            System.out.print("> 代理名称: ");
            String agentName = scanner.nextLine().trim();
            if (agentName.isEmpty()) {
                return CommandResult.failure("代理名称不能为空");
            }
            if (!agentName.matches("[a-z0-9-]+")) {
                return CommandResult.failure("代理名称必须使用 kebab-case 格式");
            }

            // 3. 输入描述
            context.getOutput().println("\n步骤 3/6: 描述代理的用途");
            context.getOutput().println("请简要说明这个代理何时使用、解决什么问题");
            System.out.print("> 描述: ");
            String description = scanner.nextLine().trim();
            if (description.isEmpty()) {
                return CommandResult.failure("描述不能为空");
            }

            // 4. 选择工具
            context.getOutput().println("\n步骤 4/6: 配置工具权限");
            context.getOutput().println("输入 '*' 允许所有工具，或者逗号分隔的工具名称");
            context.getOutput().println("常用工具: BashTool, FileReadTool, FileWriteTool, WebSearchTool");
            System.out.print("> 工具 [默认: *]: ");
            String tools = scanner.nextLine().trim();
            if (tools.isEmpty()) {
                tools = "*";
            }

            // 5. 选择模型
            context.getOutput().println("\n步骤 5/6: 指定模型（可选）");
            context.getOutput().println("直接回车使用默认模型，或输入指定模型名称");
            System.out.print("> 模型: ");
            String model = scanner.nextLine().trim();

            // 6. 输入系统提示词
            context.getOutput().println("\n步骤 6/6: 输入系统提示词");
            context.getOutput().println("定义代理的行为、职责和工作方式（多行输入，空行结束）:");
            StringBuilder systemPrompt = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty() && systemPrompt.length() > 0) {
                    break;
                }
                if (systemPrompt.length() > 0) {
                    systemPrompt.append("\n");
                }
                systemPrompt.append(line);
            }

            if (systemPrompt.length() == 0) {
                return CommandResult.failure("系统提示词不能为空");
            }

            // 7. 确认并保存
            context.getOutput().println("\n=== 代理配置预览 ===");
            context.getOutput().println("名称: " + agentName);
            context.getOutput().println("位置: " + location);
            context.getOutput().println("描述: " + description);
            context.getOutput().println("工具: " + tools);
            if (!model.isEmpty()) {
                context.getOutput().println("模型: " + model);
            }
            context.getOutput().println("系统提示词: " + systemPrompt.toString().substring(0, Math.min(100, systemPrompt.length())) + "...");

            context.getOutput().println("\n确认创建? (y/n): ");
            System.out.print("> ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (!confirm.equals("y") && !confirm.equals("yes")) {
                return CommandResult.success("已取消");
            }

            // 生成代理文件
            String agentContent = buildAgentMarkdownDetailed(
                    agentName, description, tools, systemPrompt.toString(), model);

            // 保存文件
            Path agentDir = location.equals("user")
                    ? Paths.get(System.getProperty("user.home"), ".claude", "agents")
                    : Paths.get(System.getProperty("user.dir"), ".claude", "agents");

            Files.createDirectories(agentDir);
            Path agentFile = agentDir.resolve(agentName + ".md");

            if (Files.exists(agentFile)) {
                return CommandResult.failure("代理文件已存在: " + agentFile);
            }

            Files.writeString(agentFile, agentContent);

            // 刷新注册表
            agentRegistry.reload();

            return CommandResult.success(String.format(
                    "\n✅ 代理创建成功!\n\n"
                            + "名称: %s\n"
                            + "位置: %s\n"
                            + "文件: %s\n\n"
                            + "使用: /agents run %s <任务描述>\n",
                    agentName, location, agentFile, agentName
            ));

        } catch (Exception e) {
            log.error("创建代理失败", e);
            return CommandResult.failure("创建失败: " + e.getMessage());
        }
    }

    /**
     * 构建详细的代理Markdown文件
     */
    private String buildAgentMarkdownDetailed(String name, String description,
                                              String tools, String systemPrompt, String model) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(name).append("\n\n");
        md.append("## 何时使用\n");
        md.append(description).append("\n\n");
        md.append("## 工具\n");
        md.append("- ").append(tools).append("\n\n");
        if (model != null && !model.isEmpty()) {
            md.append("## 模型\n");
            md.append(model).append("\n\n");
        }
        md.append("## 系统提示词\n");
        md.append(systemPrompt).append("\n");
        return md.toString();
    }

    /**
     * 编辑代理
     */
    private CommandResult editAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("请指定代理名称\n用法: /agents edit <代理名称>");
        }

        String agentType = args[1];

        return agentRegistry.getAgentByType(agentType)
                .map(agent -> {
                    if (agent.getLocation() == AgentLocation.BUILT_IN) {
                        return CommandResult.failure("无法编辑内置代理");
                    }

                    StringBuilder output = new StringBuilder();
                    output.append(String.format("\n编辑代理: %s\n\n", agentType));
                    output.append("可编辑选项:\n");
                    output.append("  1. 何时使用 (description)\n");
                    output.append("  2. 工具权限 (tools)\n");
                    output.append("  3. 系统提示词 (system prompt)\n");
                    output.append("  4. 指定模型 (model)\n");
                    output.append("  5. UI颜色 (color)\n\n");
                    output.append("当前版本暂不支持交互式编辑\n");
                    output.append(String.format("请手动编辑文件: %s\n", getAgentFilePath(agent)));

                    return CommandResult.success(output.toString());
                })
                .orElse(CommandResult.failure("未找到代理: " + agentType));
    }

    /**
     * 删除代理
     */
    private CommandResult deleteAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("请指定代理名称\n用法: /agents delete <代理名称>");
        }

        String agentType = args[1];
        boolean force = List.of(args).contains("--force") ||
                List.of(args).contains("-f");

        return agentRegistry.getAgentByType(agentType)
                .map(agent -> {
                    if (agent.getLocation() == AgentLocation.BUILT_IN) {
                        return CommandResult.failure("无法删除内置代理");
                    }

                    if (!force) {
                        return CommandResult.success(String.format(
                                "\n⚠️  警告: 即将删除代理 '%s'\n\n" +
                                        "位置: %s\n" +
                                        "文件: %s\n\n" +
                                        "确认删除请使用: /agents delete %s --force\n",
                                agentType,
                                agent.getLocation().getValue(),
                                getAgentFilePath(agent),
                                agentType
                        ));
                    }

                    try {
                        Path filePath = Paths.get(getAgentFilePath(agent));
                        Files.deleteIfExists(filePath);

                        // 刷新注册表
                        agentRegistry.reload();

                        return CommandResult.success(String.format(
                                "\n✅ 已删除代理: %s\n", agentType));
                    } catch (IOException e) {
                        log.error("删除代理文件失败", e);
                        return CommandResult.failure("删除失败: " + e.getMessage());
                    }
                })
                .orElse(CommandResult.failure("未找到代理: " + agentType));
    }

    /**
     * 验证代理配置
     */
    private CommandResult validateAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            // 验证所有代理
            return validateAllAgents();
        }

        String agentType = args[1];
        return agentRegistry.getAgentByType(agentType)
                .map(this::validateSingleAgent)
                .orElse(CommandResult.failure("未找到代理: " + agentType));
    }

    private CommandResult validateAllAgents() {
        List<AgentConfig> agents = agentRegistry.getAllAgents();
        StringBuilder output = new StringBuilder();
        output.append("\n=== 验证所有代理 ===\n\n");

        int validCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        for (AgentConfig agent : agents) {
            ValidationResult result = validateAgentConfig(agent);

            if (result.hasErrors()) {
                output.append(String.format("❌ %s: %d 错误\n",
                        agent.getAgentType(), result.getErrors().size()));
                errorCount++;
            } else if (result.hasWarnings()) {
                output.append(String.format("⚠️  %s: %d 警告\n",
                        agent.getAgentType(), result.getWarnings().size()));
                warningCount++;
            } else {
                output.append(String.format("✅ %s: 有效\n", agent.getAgentType()));
                validCount++;
            }
        }

        output.append(String.format("\n总结: %d 有效, %d 警告, %d 错误\n",
                validCount, warningCount, errorCount));

        return CommandResult.success(output.toString());
    }

    private CommandResult validateSingleAgent(AgentConfig agent) {
        ValidationResult result = validateAgentConfig(agent);

        StringBuilder output = new StringBuilder();
        output.append(String.format("\n=== 验证代理: %s ===\n\n", agent.getAgentType()));

        if (result.hasErrors()) {
            output.append("❌ 错误:\n");
            result.getErrors().forEach(err ->
                    output.append(String.format("  - %s\n", err)));
        }

        if (result.hasWarnings()) {
            output.append("\n⚠️  警告:\n");
            result.getWarnings().forEach(warn ->
                    output.append(String.format("  - %s\n", warn)));
        }

        if (!result.hasErrors() && !result.hasWarnings()) {
            output.append("✅ 配置有效\n");
        }

        return CommandResult.success(output.toString());
    }

    /**
     * 验证代理配置
     */
    private ValidationResult validateAgentConfig(AgentConfig agent) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 验证名称
        if (agent.getAgentType() == null || agent.getAgentType().trim().isEmpty()) {
            errors.add("代理名称不能为空");
        } else {
            if (!agent.getAgentType().matches("^[a-zA-Z][a-zA-Z0-9-]*$")) {
                errors.add("代理名称格式无效（只能包含字母、数字和连字符，且必须以字母开头）");
            }
            if (agent.getAgentType().length() < 3) {
                warnings.add("代理名称过短（建议至少3个字符）");
            }
            if (agent.getAgentType().length() > 50) {
                errors.add("代理名称过长（不能超过50个字符）");
            }
            if (RESERVED_NAMES.contains(agent.getAgentType().toLowerCase())) {
                errors.add("代理名称不能使用保留名称");
            }
        }

        // 验证描述
        if (agent.getWhenToUse() == null || agent.getWhenToUse().trim().isEmpty()) {
            errors.add("描述（whenToUse）不能为空");
        } else if (agent.getWhenToUse().length() < 10) {
            warnings.add("描述过短（建议至少10个字符）");
        }

        // 验证系统提示词
        if (agent.getSystemPrompt() == null || agent.getSystemPrompt().trim().isEmpty()) {
            errors.add("系统提示词不能为空");
        } else if (agent.getSystemPrompt().length() < 20) {
            warnings.add("系统提示词过短（建议至少20个字符以确保有效行为）");
        }

        // 验证工具
        if (!agent.allowsAllTools() && (agent.getTools() == null || agent.getTools().isEmpty())) {
            warnings.add("未选择任何工具 - 代理能力将受限");
        }

        return new ValidationResult(errors, warnings);
    }

    /**
     * 运行代理 - 执行SubAgent任务
     */
    private CommandResult runAgent(CommandContext context, String[] args) {
        if (args.length < 2) {
            return CommandResult.failure("请指定代理名称和任务\n用法: /agents run <代理名称> <任务描述>");
        }

        String agentType = args[1];

        // 获取任务描述（剩余所有参数）
        String task = args.length > 2 ?
                String.join(" ", Arrays.copyOfRange(args, 2, args.length)) :
                "";

        if (task.isEmpty()) {
            return CommandResult.failure("请提供任务描述\n用法: /agents run <代理名称> <任务描述>");
        }

        return agentRegistry.getAgentByType(agentType)
                .map(agent -> executeAgent(agent, task, context))
                .orElse(CommandResult.failure("未找到代理: " + agentType));
    }

    /**
     * 执行代理任务
     */
    private CommandResult executeAgent(AgentConfig agent, String task, CommandContext context) {

        //todo 直接依赖AgentExecutor的实现

        return null;
    }

    /**
     * 获取代理文件路径
     */
    private String getAgentFilePath(AgentConfig agent) {
        if (agent.getLocation() == AgentLocation.BUILT_IN) {
            return "<内置>";
        }

        Path baseDir = agent.getLocation() == AgentLocation.USER ?
                Paths.get(System.getProperty("user.home"), CLAUDE_DIR, AGENTS_DIR) :
                Paths.get(System.getProperty("user.dir"), CLAUDE_DIR, AGENTS_DIR);

        return baseDir.resolve(agent.getAgentType() + ".md").toString();
    }

    /**
     * 文本截断
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * 文本换行
     */
    private String wrapText(String text, int width, String indent) {
        if (text == null) return "";

        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");
        int lineLength = 0;

        for (String word : words) {
            if (lineLength + word.length() > width) {
                result.append("\n").append(indent);
                lineLength = indent.length();
            }
            if (lineLength > indent.length()) {
                result.append(" ");
                lineLength++;
            }
            result.append(word);
            lineLength += word.length();
        }

        return result.toString();
    }

    /**
     * 验证结果
     */
    private static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
