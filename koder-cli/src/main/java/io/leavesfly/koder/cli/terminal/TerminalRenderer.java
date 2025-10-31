package io.leavesfly.koder.cli.terminal;

import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * 终端渲染器
 * 使用JLine3提供终端UI功能
 */
@Slf4j
@Component
public class TerminalRenderer {

    private Terminal terminal;

    @PostConstruct
    public void init() throws IOException {
        terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        
        log.debug("终端初始化完成: {} x {}", 
                terminal.getWidth(), terminal.getHeight());
    }

    @PreDestroy
    public void cleanup() throws IOException {
        if (terminal != null) {
            terminal.close();
        }
    }

    /**
     * 打印普通文本
     */
    public void println(String text) {
        terminal.writer().println(text);
        terminal.flush();
    }

    /**
     * 打印成功信息（绿色）
     */
    public void printSuccess(String text) {
        AttributedString as = new AttributedString(text, 
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        terminal.writer().println(as.toAnsi(terminal));
        terminal.flush();
    }

    /**
     * 打印错误信息（红色）
     */
    public void printError(String text) {
        AttributedString as = new AttributedString(text, 
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        terminal.writer().println(as.toAnsi(terminal));
        terminal.flush();
    }

    /**
     * 打印警告信息（黄色）
     */
    public void printWarning(String text) {
        AttributedString as = new AttributedString(text, 
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        terminal.writer().println(as.toAnsi(terminal));
        terminal.flush();
    }

    /**
     * 打印信息文本（青色）
     */
    public void printInfo(String text) {
        AttributedString as = new AttributedString(text, 
                AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
        terminal.writer().println(as.toAnsi(terminal));
        terminal.flush();
    }

    /**
     * 清屏
     */
    public void clearScreen() {
        terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    /**
     * 获取终端实例
     */
    public Terminal getTerminal() {
        return terminal;
    }
}
