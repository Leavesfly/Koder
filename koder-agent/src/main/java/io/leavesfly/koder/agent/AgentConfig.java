package io.leavesfly.koder.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代理配置
 * 对应AGENTS.md文件中的代理定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    /**
     * 代理类型标识符
     */
    private String agentType;

    /**
     * 何时使用该代理的描述
     */
    private String whenToUse;

    /**
     * 工具权限列表（"*"表示全部工具）
     */
    private List<String> tools;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 代理位置（built-in/user/project）
     */
    private AgentLocation location;

    /**
     * UI颜色（可选）
     */
    private String color;

    /**
     * 模型名称覆盖（可选）
     */
    private String modelName;

    /**
     * 代理位置枚举
     */
    public enum AgentLocation {
        BUILT_IN("built-in"),
        USER("user"),
        PROJECT("project");

        private final String value;

        AgentLocation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 是否允许使用所有工具
     */
    public boolean allowsAllTools() {
        return tools != null && tools.size() == 1 && "*".equals(tools.get(0));
    }

    /**
     * 是否允许使用指定工具
     */
    public boolean allowsTool(String toolName) {
        if (allowsAllTools()) {
            return true;
        }
        return tools != null && tools.contains(toolName);
    }
}
