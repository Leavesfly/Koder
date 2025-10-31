package io.leavesfly.koder.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局配置类
 * 存储在 ~/.koder.json
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig {
    
    /**
     * 项目级配置映射
     */
    @JsonProperty("projects")
    @Builder.Default
    private Map<String, ProjectConfig> projects = new HashMap<>();
    
    /**
     * 启动次数统计
     */
    @JsonProperty("numStartups")
    @Builder.Default
    private int numStartups = 0;
    
    /**
     * 自动更新状态
     */
    @JsonProperty("autoUpdaterStatus")
    @Builder.Default
    private String autoUpdaterStatus = "not_configured";
    
    /**
     * 用户ID
     */
    @JsonProperty("userID")
    private String userID;
    
    /**
     * 主题设置
     */
    @JsonProperty("theme")
    @Builder.Default
    private String theme = "dark";
    
    /**
     * 是否完成引导
     */
    @JsonProperty("hasCompletedOnboarding")
    @Builder.Default
    private Boolean hasCompletedOnboarding = false;
    
    /**
     * 最后引导版本
     */
    @JsonProperty("lastOnboardingVersion")
    private String lastOnboardingVersion;
    
    /**
     * 最后查看发布说明版本
     */
    @JsonProperty("lastReleaseNotesSeen")
    private String lastReleaseNotesSeen;
    
    /**
     * MCP服务器配置
     */
    @JsonProperty("mcpServers")
    @Builder.Default
    private Map<String, McpServerConfig> mcpServers = new HashMap<>();
    
    /**
     * 首选通知渠道
     */
    @JsonProperty("preferredNotifChannel")
    @Builder.Default
    private String preferredNotifChannel = "iterm2";
    
    /**
     * 详细日志模式
     */
    @JsonProperty("verbose")
    @Builder.Default
    private boolean verbose = false;
    
    /**
     * 安全模式（需要用户确认危险操作）
     */
    @JsonProperty("safeMode")
    @Builder.Default
    private Boolean safeMode = false;
    
    /**
     * 自定义API密钥响应
     */
    @JsonProperty("customApiKeyResponses")
    @Builder.Default
    private Map<String, List<String>> customApiKeyResponses = new HashMap<>();
    
    /**
     * 主要提供商
     */
    @JsonProperty("primaryProvider")
    @Builder.Default
    private ProviderType primaryProvider = ProviderType.ANTHROPIC;
    
    /**
     * 最大Token数
     */
    @JsonProperty("maxTokens")
    private Integer maxTokens;
    
    /**
     * 是否确认成本阈值
     */
    @JsonProperty("hasAcknowledgedCostThreshold")
    @Builder.Default
    private Boolean hasAcknowledgedCostThreshold = false;
    
    /**
     * OAuth账户信息
     */
    @JsonProperty("oauthAccount")
    private AccountInfo oauthAccount;
    
    /**
     * Shift+Enter键绑定是否安装
     */
    @JsonProperty("shiftEnterKeyBindingInstalled")
    private Boolean shiftEnterKeyBindingInstalled;
    
    /**
     * 代理设置
     */
    @JsonProperty("proxy")
    private String proxy;
    
    /**
     * 流式输出
     */
    @JsonProperty("stream")
    @Builder.Default
    private Boolean stream = true;
    
    /**
     * 模型配置列表
     */
    @JsonProperty("modelProfiles")
    @Builder.Default
    private List<ModelProfile> modelProfiles = new ArrayList<>();
    
    /**
     * 模型指针
     */
    @JsonProperty("modelPointers")
    private ModelPointers modelPointers;
    
    /**
     * 默认模型名称
     */
    @JsonProperty("defaultModelName")
    private String defaultModelName;
    
    /**
     * 最后忽略的更新版本
     */
    @JsonProperty("lastDismissedUpdateVersion")
    private String lastDismissedUpdateVersion;
}

/**
 * 账户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AccountInfo {
    
    @JsonProperty("accountUuid")
    private String accountUuid;
    
    @JsonProperty("emailAddress")
    private String emailAddress;
    
    @JsonProperty("organizationUuid")
    private String organizationUuid;
}
