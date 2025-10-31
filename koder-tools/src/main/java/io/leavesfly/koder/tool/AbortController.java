package io.leavesfly.koder.tool;

/**
 * 中断控制器
 * 用于控制长时间运行操作的取消
 */
public class AbortController {

    private volatile boolean aborted = false;
    private volatile String reason;

    /**
     * 中断操作
     */
    public void abort() {
        abort(null);
    }

    /**
     * 中断操作并指定原因
     *
     * @param reason 中断原因
     */
    public void abort(String reason) {
        this.aborted = true;
        this.reason = reason;
    }

    /**
     * 检查是否已中断
     *
     * @return true表示已中断
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * 获取中断原因
     *
     * @return 中断原因
     */
    public String getReason() {
        return reason;
    }

    /**
     * 重置状态
     */
    public void reset() {
        this.aborted = false;
        this.reason = null;
    }
}
