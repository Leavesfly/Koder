package io.leavesfly.koder.examples;

/**
 * Bash模式演示
 * 
 * 演示如何在Koder对话模式下使用!前缀直接运行shell命令
 * 
 * @author Koder Team
 */
public class BashModeDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Koder Bash模式使用演示");
        System.out.println("=".repeat(60));
        System.out.println();
        
        demonstrateBasicUsage();
        demonstrateAdvancedUsage();
        demonstrateSecurity();
        demonstrateTips();
    }

    /**
     * 基础用法演示
     */
    private static void demonstrateBasicUsage() {
        System.out.println("【基础用法】");
        System.out.println("-".repeat(60));
        System.out.println("在Koder REPL中，输入!前缀可以直接运行shell命令：");
        System.out.println();
        
        System.out.println("示例 1: 列出当前目录文件");
        System.out.println("  输入: !ls -la");
        System.out.println("  效果: 直接执行ls -la命令并显示结果");
        System.out.println();
        
        System.out.println("示例 2: 查看当前工作目录");
        System.out.println("  输入: !pwd");
        System.out.println("  效果: 显示当前工作目录的完整路径");
        System.out.println();
        
        System.out.println("示例 3: 查看文件内容");
        System.out.println("  输入: !cat README.md");
        System.out.println("  效果: 显示README.md文件的内容");
        System.out.println();
    }

    /**
     * 高级用法演示
     */
    private static void demonstrateAdvancedUsage() {
        System.out.println("【高级用法】");
        System.out.println("-".repeat(60));
        System.out.println();
        
        System.out.println("示例 1: 使用管道");
        System.out.println("  输入: !ls -la | grep java");
        System.out.println("  效果: 列出所有包含'java'的文件");
        System.out.println();
        
        System.out.println("示例 2: 查找文件");
        System.out.println("  输入: !find . -name '*.java'");
        System.out.println("  效果: 递归查找所有.java文件");
        System.out.println();
        
        System.out.println("示例 3: Git操作");
        System.out.println("  输入: !git status");
        System.out.println("  效果: 查看Git仓库状态");
        System.out.println();
        
        System.out.println("示例 4: Maven构建");
        System.out.println("  输入: !mvn clean package");
        System.out.println("  效果: 执行Maven构建");
        System.out.println();
    }

    /**
     * 安全限制演示
     */
    private static void demonstrateSecurity() {
        System.out.println("【安全限制】");
        System.out.println("-".repeat(60));
        System.out.println("以下危险命令被禁止执行：");
        System.out.println();
        
        String[] bannedCommands = {
            "!rm -rf /",
            "!shutdown",
            "!reboot",
            "!format",
            "!dd",
            "!mkfs",
            "!fdisk"
        };
        
        for (String cmd : bannedCommands) {
            System.out.println("  ❌ " + cmd + " (被安全机制拦截)");
        }
        System.out.println();
        System.out.println("这些命令会返回错误消息：");
        System.out.println("  \"命令 'xxx' 因安全原因被禁止执行\"");
        System.out.println();
    }

    /**
     * 使用技巧
     */
    private static void demonstrateTips() {
        System.out.println("【使用技巧】");
        System.out.println("-".repeat(60));
        System.out.println();
        
        System.out.println("技巧 1: 快速切换");
        System.out.println("  - 在对话模式下输入!前缀即可运行shell命令");
        System.out.println("  - 无需切换到专门的shell模式");
        System.out.println();
        
        System.out.println("技巧 2: 命令超时");
        System.out.println("  - 默认超时时间：120秒(2分钟)");
        System.out.println("  - 最大超时时间：600秒(10分钟)");
        System.out.println("  - 长时间运行的命令会被自动中断");
        System.out.println();
        
        System.out.println("技巧 3: 输出限制");
        System.out.println("  - 标准输出和错误输出最多显示1000行");
        System.out.println("  - 超出部分会显示\"...(输出被截断)\"提示");
        System.out.println();
        
        System.out.println("技巧 4: 查看退出码");
        System.out.println("  - 命令执行失败时会显示退出码");
        System.out.println("  - 退出码0表示成功，非0表示失败");
        System.out.println();
        
        System.out.println("技巧 5: 结合AI助手");
        System.out.println("  - 可以先用!命令查看系统状态");
        System.out.println("  - 然后向AI助手咨询相关问题");
        System.out.println("  - 例如：!git log 后询问\"如何回退到上一个提交?\"");
        System.out.println();
    }
}
