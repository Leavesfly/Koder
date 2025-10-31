package io.leavesfly.koder.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * 配置管理服务
 * 负责加载、合并和持久化全局配置和项目配置
 */
@Slf4j
@Service
public class ConfigManager {
    
    private static final String GLOBAL_CONFIG_FILE = ".koder.json";
    private static final String PROJECT_CONFIG_FILE = ".koder.json";
    
    private final ObjectMapper objectMapper;
    private GlobalConfig globalConfig;
    private ProjectConfig projectConfig;
    private final Path homeDir;
    private Path projectDir;
    
    public ConfigManager() {
        this.objectMapper = createObjectMapper();
        this.homeDir = Paths.get(System.getProperty("user.home"));
        this.globalConfig = new GlobalConfig();
        this.projectConfig = new ProjectConfig();
    }
    
    /**
     * 创建配置的ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    /**
     * 初始化配置系统
     * 加载全局配置和项目配置
     */
    public void initialize(Path workingDirectory) {
        this.projectDir = workingDirectory;
        
        // 加载全局配置
        loadGlobalConfig();
        
        // 加载项目配置
        loadProjectConfig();
        
        log.info("配置系统初始化完成");
    }
    
    /**
     * 加载全局配置
     */
    private void loadGlobalConfig() {
        Path globalConfigPath = homeDir.resolve(GLOBAL_CONFIG_FILE);
        
        if (Files.exists(globalConfigPath)) {
            try {
                globalConfig = objectMapper.readValue(
                    globalConfigPath.toFile(), 
                    GlobalConfig.class
                );
                log.info("已加载全局配置: {}", globalConfigPath);
            } catch (IOException e) {
                log.error("加载全局配置失败，使用默认配置", e);
                globalConfig = createDefaultGlobalConfig();
            }
        } else {
            log.info("全局配置文件不存在，使用默认配置");
            globalConfig = createDefaultGlobalConfig();
        }
    }
    
    /**
     * 加载项目配置
     */
    private void loadProjectConfig() {
        if (projectDir == null) {
            log.warn("项目目录未设置，跳过项目配置加载");
            return;
        }
        
        Path projectConfigPath = projectDir.resolve(PROJECT_CONFIG_FILE);
        
        if (Files.exists(projectConfigPath)) {
            try {
                projectConfig = objectMapper.readValue(
                    projectConfigPath.toFile(), 
                    ProjectConfig.class
                );
                log.info("已加载项目配置: {}", projectConfigPath);
            } catch (IOException e) {
                log.error("加载项目配置失败，使用默认配置", e);
                projectConfig = createDefaultProjectConfig();
            }
        } else {
            log.info("项目配置文件不存在，使用默认配置");
            projectConfig = createDefaultProjectConfig();
        }
    }
    
    /**
     * 保存全局配置
     */
    public void saveGlobalConfig() {
        Path globalConfigPath = homeDir.resolve(GLOBAL_CONFIG_FILE);
        saveConfigToFile(globalConfig, globalConfigPath);
    }
    
    /**
     * 保存项目配置
     */
    public void saveProjectConfig() {
        if (projectDir == null) {
            log.warn("项目目录未设置，无法保存项目配置");
            return;
        }
        
        Path projectConfigPath = projectDir.resolve(PROJECT_CONFIG_FILE);
        saveConfigToFile(projectConfig, projectConfigPath);
    }
    
    /**
     * 原子方式保存配置到文件
     */
    private void saveConfigToFile(Object config, Path targetPath) {
        try {
            // 先写入临时文件
            Path tempFile = Files.createTempFile("koder-config-", ".tmp");
            objectMapper.writeValue(tempFile.toFile(), config);
            
            // 原子性移动到目标位置
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("配置已保存: {}", targetPath);
        } catch (IOException e) {
            log.error("保存配置失败: {}", targetPath, e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
    
    /**
     * 创建默认全局配置
     */
    private GlobalConfig createDefaultGlobalConfig() {
        return GlobalConfig.builder()
            .theme("dark")
            .verbose(false)
            .stream(true)
            .primaryProvider(ProviderType.ANTHROPIC)
            .preferredNotifChannel("iterm2")
            .hasCompletedOnboarding(false)
            .hasAcknowledgedCostThreshold(false)
            .build();
    }
    
    /**
     * 创建默认项目配置
     */
    private ProjectConfig createDefaultProjectConfig() {
        return ProjectConfig.builder()
            .dontCrawlDirectory(false)
            .enableArchitectTool(false)
            .hasTrustDialogAccepted(false)
            .build();
    }
    
    /**
     * 获取全局配置
     */
    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }
    
    /**
     * 获取项目配置
     */
    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }
    
    /**
     * 更新全局配置
     */
    public void updateGlobalConfig(GlobalConfig config) {
        this.globalConfig = config;
    }
    
    /**
     * 更新项目配置
     */
    public void updateProjectConfig(ProjectConfig config) {
        this.projectConfig = config;
    }
    
    /**
     * 根据模型名称查找模型配置
     */
    public Optional<ModelProfile> findModelProfile(String modelName) {
        return globalConfig.getModelProfiles().stream()
            .filter(profile -> profile.getModelName().equals(modelName))
            .findFirst();
    }
    
    /**
     * 获取当前激活的模型配置列表
     */
    public java.util.List<ModelProfile> getActiveModelProfiles() {
        return globalConfig.getModelProfiles().stream()
            .filter(ModelProfile::isActive)
            .toList();
    }
    
    /**
     * 检查工具是否已授权
     */
    public boolean isToolAllowed(String toolKey) {
        return projectConfig.getAllowedTools().contains(toolKey);
    }
    
    /**
     * 添加工具授权
     */
    public void allowTool(String toolKey) {
        projectConfig.getAllowedTools().add(toolKey);
        saveProjectConfig();
    }
    
    /**
     * 移除工具授权
     */
    public void disallowTool(String toolKey) {
        projectConfig.getAllowedTools().remove(toolKey);
        saveProjectConfig();
    }
}
