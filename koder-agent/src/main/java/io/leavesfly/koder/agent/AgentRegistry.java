package io.leavesfly.koder.agent;

import io.leavesfly.koder.agent.loader.AgentLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理注册表
 * 管理所有可用的代理配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRegistry {

    private final AgentLoader agentLoader;
    
    // 代理缓存
    private final Map<String, AgentConfig> agents = new ConcurrentHashMap<>();
    
    // 当前工作目录
    private String currentWorkingDirectory = System.getProperty("user.dir");

    /**
     * 初始化注册表
     */
    public void initialize() {
        reload();
    }

    /**
     * 重新加载所有代理
     */
    public void reload() {
        log.info("重新加载代理配置...");
        
        Map<String, AgentConfig> loadedAgents = agentLoader.loadAllAgents(currentWorkingDirectory);
        
        agents.clear();
        agents.putAll(loadedAgents);
        
        log.info("成功加载 {} 个代理", agents.size());
    }

    /**
     * 获取所有代理
     */
    public List<AgentConfig> getAllAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * 根据类型获取代理
     */
    public Optional<AgentConfig> getAgentByType(String agentType) {
        return Optional.ofNullable(agents.get(agentType));
    }

    /**
     * 获取所有代理类型
     */
    public List<String> getAvailableAgentTypes() {
        return new ArrayList<>(agents.keySet());
    }

    /**
     * 检查代理是否存在
     */
    public boolean hasAgent(String agentType) {
        return agents.containsKey(agentType);
    }

    /**
     * 设置工作目录
     */
    public void setWorkingDirectory(String directory) {
        this.currentWorkingDirectory = directory;
        reload(); // 切换目录后重新加载
    }

    /**
     * 获取当前工作目录
     */
    public String getWorkingDirectory() {
        return currentWorkingDirectory;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        agents.clear();
    }
}
