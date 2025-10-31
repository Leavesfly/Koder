package io.leavesfly.koder.cli.repl;

import io.leavesfly.koder.core.cost.CostTracker;

import io.leavesfly.koder.core.message.Message;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REPL会话
 * 管理对话历史和会话状态
 */
@Data
public class REPLSession {

    /**
     * 会话ID
     */
    private final String sessionId;

    /**
     * 消息历史
     */
    private final List<Message> messages = new ArrayList<>();

    /**
     * 是否正在加载
     */
    private final AtomicBoolean loading = new AtomicBoolean(false);

    /**
     * 开始时间
     */
    private final long startTime;

    /**
     * 成本追踪器
     */
    private final CostTracker costTracker;

    /**
     * 当前工作目录
     */
    private String workingDirectory;

    /**
     * 安全模式
     */
    private boolean safeMode;

    /**
     * 详细模式
     */
    private boolean verbose;

    public REPLSession(String sessionId) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
        this.costTracker = new CostTracker(sessionId);
        this.workingDirectory = System.getProperty("user.dir");
        this.safeMode = false;
        this.verbose = false;
    }

    /**
     * 添加消息
     */
    public void addMessage(Message message) {
        messages.add(message);
    }

    /**
     * 清空消息历史
     */
    public void clearMessages() {
        messages.clear();
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * 设置加载状态
     */
    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    /**
     * 检查是否正在加载
     */
    public boolean isLoading() {
        return loading.get();
    }
}
