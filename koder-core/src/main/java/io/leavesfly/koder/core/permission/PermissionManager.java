package io.leavesfly.koder.core.permission;

import io.leavesfly.koder.core.config.ConfigManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 权限管理服务
 * 负责工具权限的检查和授权管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionManager {
    
    private final ConfigManager configManager;
    
    // 安全命令白名单（无需授权）
    private static final Set<String> SAFE_COMMANDS = Set.of(
        "git status", "git diff", "git log", "git branch",
        "pwd", "tree", "date", "which"
    );
    
    // 会话级权限（内存存储）
    private final Set<String> sessionPermissions = new HashSet<>();
    
    /**
     * 检查工具是否需要权限
     */
    public boolean needsPermission(String toolName, Object input, boolean safeMode) {
        // YOLO模式（非安全模式）下允许所有工具
        if (!safeMode) {
            return false;
        }
        
        // 构造权限键
        String permissionKey = buildPermissionKey(toolName, input);
        
        // 检查是否已授权
        return !isPermissionGranted(permissionKey);
    }
    
    /**
     * 检查Bash命令权限
     */
    public boolean checkBashPermission(String command, boolean safeMode) {
        if (!safeMode) {
            return true;
        }
        
        // 检查安全命令白名单
        if (SAFE_COMMANDS.contains(command)) {
            return true;
        }
        
        // 检查完全匹配权限
        String exactKey = "bash(" + command + ")";
        if (isPermissionGranted(exactKey)) {
            return true;
        }
        
        // 检查前缀匹配权限
        String commandPrefix = extractCommandPrefix(command);
        if (commandPrefix != null) {
            String prefixKey = "bash(" + commandPrefix + ":*)";
            if (isPermissionGranted(prefixKey)) {
                return true;
            }
        }
        
        // 检查全局Bash权限
        return isPermissionGranted("bash");
    }
    
    /**
     * 检查文件系统权限
     */
    public boolean checkFilePermission(String path, String operation, boolean safeMode) {
        if (!safeMode) {
            return true;
        }
        
        String permissionKey = operation + "(" + path + ")";
        return isPermissionGranted(permissionKey);
    }
    
    /**
     * 授予权限
     */
    public void grantPermission(String permissionKey, boolean persistent) {
        if (persistent) {
            // 持久化权限
            configManager.allowTool(permissionKey);
            log.info("已授予持久化权限: {}", permissionKey);
        } else {
            // 会话级权限
            sessionPermissions.add(permissionKey);
            log.info("已授予会话权限: {}", permissionKey);
        }
    }
    
    /**
     * 撤销权限
     */
    public void revokePermission(String permissionKey) {
        configManager.disallowTool(permissionKey);
        sessionPermissions.remove(permissionKey);
        log.info("已撤销权限: {}", permissionKey);
    }
    
    /**
     * 检查权限是否已授予
     */
    private boolean isPermissionGranted(String permissionKey) {
        // 检查会话级权限
        if (sessionPermissions.contains(permissionKey)) {
            return true;
        }
        
        // 检查持久化权限
        return configManager.isToolAllowed(permissionKey);
    }
    
    /**
     * 构造权限键
     */
    private String buildPermissionKey(String toolName, Object input) {
        if (input == null) {
            return toolName;
        }
        return toolName + "(" + input + ")";
    }
    
    /**
     * 提取命令前缀
     * 例如: "git status" -> "git"
     */
    private String extractCommandPrefix(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = command.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }
    
    /**
     * 清除会话权限
     */
    public void clearSessionPermissions() {
        sessionPermissions.clear();
        log.info("已清除所有会话权限");
    }
}
