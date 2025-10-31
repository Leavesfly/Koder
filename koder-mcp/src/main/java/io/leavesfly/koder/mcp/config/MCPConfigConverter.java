package io.leavesfly.koder.mcp.config;

import io.leavesfly.koder.core.config.McpServerConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP配置转换器
 * 将core模块的配置转换为mcp模块的配置
 */
@Slf4j
@UtilityClass
public class MCPConfigConverter {
    
    /**
     * 转换单个MCP服务器配置
     */
    public MCPServerConfig convert(String name, McpServerConfig coreConfig) {
        if (coreConfig == null) {
            return null;
        }
        
        String type = coreConfig.getType();
        Map<String, String> env = coreConfig.getEnv();
        
        if ("stdio".equals(type)) {
            // 尝试从配置中提取command和args
            Object commandObj = getProperty(coreConfig, "command");
            Object argsObj = getProperty(coreConfig, "args");
            
            if (commandObj == null) {
                log.warn("Stdio类型的MCP服务器缺少command配置: {}", name);
                return null;
            }
            
            String command = commandObj.toString();
            List<String> args = new ArrayList<>();
            
            if (argsObj instanceof List<?>) {
                for (Object arg : (List<?>) argsObj) {
                    args.add(arg.toString());
                }
            }
            
            return MCPServerConfig.builder()
                    .name(name)
                    .type(type)
                    .command(command)
                    .args(args.toArray(new String[0]))
                    .env(env != null ? env : Map.of())
                    .build();
                    
        } else if ("sse".equals(type)) {
            // 尝试从配置中提取url
            Object urlObj = getProperty(coreConfig, "url");
            
            if (urlObj == null) {
                log.warn("SSE类型的MCP服务器缺少url配置: {}", name);
                return null;
            }
            
            return MCPServerConfig.builder()
                    .name(name)
                    .type(type)
                    .url(urlObj.toString())
                    .env(env != null ? env : Map.of())
                    .build();
        }
        
        log.warn("未知的MCP服务器类型: {}", type);
        return null;
    }
    
    /**
     * 通过反射获取属性值
     */
    private Object getProperty(McpServerConfig config, String propertyName) {
        try {
            // 使用反射获取字段值
            java.lang.reflect.Field field = config.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            return field.get(config);
        } catch (Exception e) {
            // 尝试使用getter方法
            try {
                String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + 
                                  propertyName.substring(1);
                java.lang.reflect.Method method = config.getClass().getMethod(methodName);
                return method.invoke(config);
            } catch (Exception ex) {
                log.debug("无法获取属性 {} 从配置对象", propertyName);
                return null;
            }
        }
    }
}
