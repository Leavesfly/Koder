package io.leavesfly.koder.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 项目配置类
 * 存储在项目目录下的 .koder.json
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConfig {
    
    /**
     * 已授权工具列表
     */
    @JsonProperty("allowedTools")
    @Builder.Default
    private Set<String> allowedTools = new HashSet<>();
    
    /**
     * 上下文键值对
     */
    @JsonProperty("context")
    @Builder.Default
    private Map<String, String> context = new HashMap<>();
    
    /**
     * 上下文文件列表
     */
    @JsonProperty("contextFiles")
    @Builder.Default
    private List<String> contextFiles = new ArrayList<>();
    
    /**
     * 历史记录
     */
    @JsonProperty("history")
    @Builder.Default
    private List<String> history = new ArrayList<>();
    
    /**
     * 是否禁止爬取目录
     */
    @JsonProperty("dontCrawlDirectory")
    @Builder.Default
    private boolean dontCrawlDirectory = false;
    
    /**
     * 是否启用架构工具
     */
    @JsonProperty("enableArchitectTool")
    @Builder.Default
    private boolean enableArchitectTool = false;
    
    /**
     * MCP上下文URI列表
     */
    @JsonProperty("mcpContextUris")
    @Builder.Default
    private List<String> mcpContextUris = new ArrayList<>();
    
    /**
     * 项目级MCP服务器配置
     */
    @JsonProperty("mcpServers")
    @Builder.Default
    private Map<String, McpServerConfig> mcpServers = new HashMap<>();
    
    /**
     * 已批准的.mcprc服务器
     */
    @JsonProperty("approvedMcprcServers")
    @Builder.Default
    private List<String> approvedMcprcServers = new ArrayList<>();
    
    /**
     * 已拒绝的.mcprc服务器
     */
    @JsonProperty("rejectedMcprcServers")
    @Builder.Default
    private List<String> rejectedMcprcServers = new ArrayList<>();
    
    /**
     * 最后API调用持续时间
     */
    @JsonProperty("lastAPIDuration")
    private Long lastAPIDuration;
    
    /**
     * 最后成本
     */
    @JsonProperty("lastCost")
    private Double lastCost;
    
    /**
     * 最后持续时间
     */
    @JsonProperty("lastDuration")
    private Long lastDuration;
    
    /**
     * 最后会话ID
     */
    @JsonProperty("lastSessionId")
    private String lastSessionId;
    
    /**
     * 示例文件列表
     */
    @JsonProperty("exampleFiles")
    @Builder.Default
    private List<String> exampleFiles = new ArrayList<>();
    
    /**
     * 示例文件生成时间
     */
    @JsonProperty("exampleFilesGeneratedAt")
    private Long exampleFilesGeneratedAt;
    
    /**
     * 信任对话框是否已接受
     */
    @JsonProperty("hasTrustDialogAccepted")
    @Builder.Default
    private boolean hasTrustDialogAccepted = false;
    
    /**
     * 项目引导是否完成
     */
    @JsonProperty("hasCompletedProjectOnboarding")
    @Builder.Default
    private Boolean hasCompletedProjectOnboarding = false;
}
