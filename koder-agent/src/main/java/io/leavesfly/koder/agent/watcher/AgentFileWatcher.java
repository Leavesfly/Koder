package io.leavesfly.koder.agent.watcher;

import io.leavesfly.koder.agent.AgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 代理文件监听器
 * 监控代理配置文件变化，实现热重载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentFileWatcher {

    private static final String USER_HOME = System.getProperty("user.home");
    
    private final AgentRegistry agentRegistry;
    private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean watching = new AtomicBoolean(false);
    
    private WatchService watchService;
    private final List<Path> watchedDirectories = new ArrayList<>();

    /**
     * 启动文件监听
     */
    public void start() {
        if (watching.get()) {
            log.warn("代理文件监听器已在运行");
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            
            // 注册所有需要监听的目录
            registerWatchDirectories();
            
            watching.set(true);
            
            // 启动监听线程
            watcherExecutor.submit(this::watchLoop);
            
            log.info("代理文件监听器已启动，监控 {} 个目录", watchedDirectories.size());
            
        } catch (IOException e) {
            log.error("启动代理文件监听器失败", e);
        }
    }

    /**
     * 停止文件监听
     */
    public void stop() {
        if (!watching.get()) {
            return;
        }

        watching.set(false);
        
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            log.error("关闭文件监听器失败", e);
        }
        
        watcherExecutor.shutdown();
        watchedDirectories.clear();
        
        log.info("代理文件监听器已停止");
    }

    /**
     * 注册监听目录
     */
    private void registerWatchDirectories() {
        String workingDirectory = agentRegistry.getWorkingDirectory();
        
        // 监听的目录列表
        List<Path> directories = List.of(
                Paths.get(USER_HOME, ".claude", "agents"),
                Paths.get(USER_HOME, ".kode", "agents"),
                Paths.get(workingDirectory, ".claude", "agents"),
                Paths.get(workingDirectory, ".kode", "agents")
        );

        for (Path directory : directories) {
            try {
                if (Files.exists(directory) && Files.isDirectory(directory)) {
                    directory.register(
                            watchService,
                            ENTRY_CREATE,
                            ENTRY_MODIFY,
                            ENTRY_DELETE
                    );
                    watchedDirectories.add(directory);
                    log.debug("注册监听目录: {}", directory);
                } else {
                    log.debug("跳过不存在的目录: {}", directory);
                }
            } catch (IOException e) {
                log.warn("注册监听目录失败: {}", directory, e);
            }
        }
    }

    /**
     * 监听循环
     */
    private void watchLoop() {
        log.info("代理文件监听循环已启动");
        
        while (watching.get()) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    
                    // 只处理.md文件
                    if (filename.toString().endsWith(".md")) {
                        handleFileChange(kind, filename);
                    }
                }
                
                // 重置key，继续监听
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("监听key失效，停止监听");
                    break;
                }
                
            } catch (InterruptedException e) {
                log.info("监听循环被中断");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("处理文件变化事件失败", e);
            }
        }
        
        log.info("代理文件监听循环已停止");
    }

    /**
     * 处理文件变化
     */
    private void handleFileChange(WatchEvent.Kind<?> kind, Path filename) {
        String eventType = kind == ENTRY_CREATE ? "创建" :
                          kind == ENTRY_MODIFY ? "修改" :
                          kind == ENTRY_DELETE ? "删除" : "未知";
        
        log.info("🔄 检测到代理配置文件{}: {}", eventType, filename);
        
        // 延迟一下，避免文件还在写入时就重新加载
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 重新加载所有代理
        agentRegistry.reload();
    }

    /**
     * 是否正在监听
     */
    public boolean isWatching() {
        return watching.get();
    }
}
