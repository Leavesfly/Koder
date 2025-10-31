package io.leavesfly.koder.core.context;

import io.leavesfly.koder.core.config.ConfigManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文管理服务
 * 负责管理项目上下文信息和文件内容
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextManager {
    
    private final ConfigManager configManager;
    
    // 上下文缓存
    private final Map<String, String> contextCache = new HashMap<>();
    
    /**
     * 获取项目上下文
     */
    public Map<String, String> getProjectContext() {
        Map<String, String> context = new HashMap<>(configManager.getProjectConfig().getContext());
        
        // 加载上下文文件
        List<String> contextFiles = configManager.getProjectConfig().getContextFiles();
        for (String filePath : contextFiles) {
            try {
                String content = loadFileContent(Path.of(filePath));
                context.put(filePath, content);
            } catch (IOException e) {
                log.warn("无法加载上下文文件: {}", filePath, e);
            }
        }
        
        return context;
    }
    
    /**
     * 设置上下文键值对
     */
    public void setContext(String key, String value) {
        configManager.getProjectConfig().getContext().put(key, value);
        configManager.saveProjectConfig();
        
        // 更新缓存
        contextCache.put(key, value);
        
        log.info("已设置上下文: {} = {}", key, value);
    }
    
    /**
     * 获取上下文值
     */
    public String getContext(String key) {
        // 优先从缓存获取
        if (contextCache.containsKey(key)) {
            return contextCache.get(key);
        }
        
        // 从配置获取
        String value = configManager.getProjectConfig().getContext().get(key);
        if (value != null) {
            contextCache.put(key, value);
        }
        
        return value;
    }
    
    /**
     * 删除上下文
     */
    public void removeContext(String key) {
        configManager.getProjectConfig().getContext().remove(key);
        contextCache.remove(key);
        configManager.saveProjectConfig();
        
        log.info("已删除上下文: {}", key);
    }
    
    /**
     * 添加上下文文件
     */
    public void addContextFile(String filePath) {
        List<String> contextFiles = configManager.getProjectConfig().getContextFiles();
        if (!contextFiles.contains(filePath)) {
            contextFiles.add(filePath);
            configManager.saveProjectConfig();
            log.info("已添加上下文文件: {}", filePath);
        }
    }
    
    /**
     * 移除上下文文件
     */
    public void removeContextFile(String filePath) {
        configManager.getProjectConfig().getContextFiles().remove(filePath);
        configManager.saveProjectConfig();
        log.info("已移除上下文文件: {}", filePath);
    }
    
    /**
     * 加载文件内容（带缓存）
     */
    private String loadFileContent(Path filePath) throws IOException {
        String cacheKey = "file:" + filePath.toString();
        
        // 检查缓存
        if (contextCache.containsKey(cacheKey)) {
            return contextCache.get(cacheKey);
        }
        
        // 读取文件
        String content = Files.readString(filePath);
        contextCache.put(cacheKey, content);
        
        return content;
    }
    
    /**
     * 清除上下文缓存
     */
    public void clearCache() {
        contextCache.clear();
        log.info("已清除上下文缓存");
    }
}
