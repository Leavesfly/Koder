package io.leavesfly.koder.agent.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.leavesfly.koder.agent.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 代理加载器
 * 从Markdown文件加载代理配置（支持YAML frontmatter）
 */
@Slf4j
@Component
public class AgentLoader {

    private static final String USER_HOME = System.getProperty("user.home");
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$",
            Pattern.DOTALL
    );

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 加载所有代理配置
     */
    public Map<String, AgentConfig> loadAllAgents(String workingDirectory) {
        Map<String, AgentConfig> agentMap = new LinkedHashMap<>();

        // 1. 内置代理（最低优先级）
        // 1.1 通用代理（fallback）
        agentMap.put("general-purpose", createBuiltinGeneralPurposeAgent());
        
        // 1.2 专用内置代理
        for (AgentConfig builtinAgent : BuiltinAgents.getAllBuiltinAgents()) {
            agentMap.put(builtinAgent.getAgentType(), builtinAgent);
        }

        // 2. ~/.claude/agents（Claude Code用户目录兼容）
        loadAgentsFromDirectory(
                Paths.get(USER_HOME, ".claude", "agents"),
                AgentConfig.AgentLocation.USER,
                agentMap
        );

        // 3. ~/.kode/agents（Kode用户目录）
        loadAgentsFromDirectory(
                Paths.get(USER_HOME, ".kode", "agents"),
                AgentConfig.AgentLocation.USER,
                agentMap
        );

        // 4. ./.claude/agents（Claude Code项目目录兼容）
        loadAgentsFromDirectory(
                Paths.get(workingDirectory, ".claude", "agents"),
                AgentConfig.AgentLocation.PROJECT,
                agentMap
        );

        // 5. ./.kode/agents（Kode项目目录，最高优先级）
        loadAgentsFromDirectory(
                Paths.get(workingDirectory, ".kode", "agents"),
                AgentConfig.AgentLocation.PROJECT,
                agentMap
        );

        log.info("加载了 {} 个代理配置", agentMap.size());
        return agentMap;
    }

    /**
     * 从目录加载代理
     */
    private void loadAgentsFromDirectory(
            Path directory,
            AgentConfig.AgentLocation location,
            Map<String, AgentConfig> agentMap) {

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.debug("代理目录不存在: {}", directory);
            return;
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(".md"))
                    .forEach(path -> {
                        try {
                            AgentConfig agent = loadAgentFromFile(path, location);
                            if (agent != null) {
                                // 后加载的覆盖先加载的（优先级）
                                agentMap.put(agent.getAgentType(), agent);
                                log.debug("加载代理: {} (来自 {})", agent.getAgentType(), path);
                            }
                        } catch (Exception e) {
                            log.warn("加载代理文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("扫描代理目录失败: {}", directory, e);
        }
    }

    /**
     * 从文件加载单个代理
     */
    private AgentConfig loadAgentFromFile(Path filePath, AgentConfig.AgentLocation location) throws IOException {
        String content = Files.readString(filePath);

        // 解析frontmatter
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.matches()) {
            log.warn("文件缺少YAML frontmatter: {}", filePath);
            return null;
        }

        String frontmatterYaml = matcher.group(1);
        String systemPrompt = matcher.group(2).trim();

        // 解析YAML frontmatter
        @SuppressWarnings("unchecked")
        Map<String, Object> frontmatter = yamlMapper.readValue(frontmatterYaml, Map.class);

        // 验证必需字段
        String name = (String) frontmatter.get("name");
        String description = (String) frontmatter.get("description");

        if (name == null || description == null) {
            log.warn("代理缺少必需字段 (name, description): {}", filePath);
            return null;
        }

        // 解析工具列表
        List<String> tools = parseTools(frontmatter.get("tools"));

        // 构建代理配置
        AgentConfig.AgentConfigBuilder builder = AgentConfig.builder()
                .agentType(name)
                .whenToUse(description.replace("\\n", "\n"))
                .tools(tools)
                .systemPrompt(systemPrompt)
                .location(location);

        // 可选字段
        if (frontmatter.containsKey("color")) {
            builder.color((String) frontmatter.get("color"));
        }

        // 只使用model_name字段，忽略已弃用的model字段
        if (frontmatter.containsKey("model_name")) {
            builder.modelName((String) frontmatter.get("model_name"));
        }

        return builder.build();
    }

    /**
     * 解析工具列表
     */
    @SuppressWarnings("unchecked")
    private List<String> parseTools(Object toolsObj) {
        if (toolsObj == null) {
            return List.of("*");
        }

        if (toolsObj instanceof String) {
            String toolsStr = (String) toolsObj;
            return "*".equals(toolsStr) ? List.of("*") : List.of(toolsStr);
        }

        if (toolsObj instanceof List) {
            List<?> toolsList = (List<?>) toolsObj;
            List<String> tools = new ArrayList<>();
            for (Object item : toolsList) {
                if (item instanceof String) {
                    tools.add((String) item);
                }
            }
            return tools.isEmpty() ? List.of("*") : tools;
        }

        return List.of("*");
    }

    /**
     * 创建内置通用代理
     */
    private AgentConfig createBuiltinGeneralPurposeAgent() {
        return AgentConfig.builder()
                .agentType("general-purpose")
                .whenToUse("General-purpose agent for researching complex questions, searching for code, and executing multi-step tasks")
                .tools(List.of("*"))
                .systemPrompt("""
                        你是一个通用代理。给定用户的任务，使用可用的工具高效彻底地完成它。
                        
                        何时使用你的能力：
                        - 在大型代码库中搜索代码、配置和模式
                        - 分析多个文件以理解系统架构
                        - 调查需要探索多个文件的复杂问题
                        - 执行多步骤研究任务
                        
                        指南：
                        - 对于文件搜索：当需要广泛搜索时使用Grep或Glob。当知道具体文件路径时使用FileRead。
                        - 对于分析：从广泛开始，然后缩小范围。如果第一次没有产生结果，使用多种搜索策略。
                        - 要彻底：检查多个位置，考虑不同的命名约定，寻找相关文件。
                        - 直接使用你的能力完成任务。
                        """)
                .location(AgentConfig.AgentLocation.BUILT_IN)
                .build();
    }

    /**
     * 获取内置通用代理（保持向后兼容）
     */
    public AgentConfig getBuiltinGeneralPurposeAgent() {
        return createBuiltinGeneralPurposeAgent();
    }
}
