# Koder Bash 模式使用指南

## 概述

Koder 支持在对话模式下直接运行 shell 命令，无需切换到专门的 shell 模式。只需在命令前加上 `!` 前缀即可。

## 快速开始

### 基本语法

```
koder> !<shell命令>
```

### 示例

```bash
# 列出文件
koder> !ls -la

# 查看当前目录
koder> !pwd

# 查看文件内容
koder> !cat README.md

# Git 操作
koder> !git status
koder> !git log --oneline

# Maven 构建
koder> !mvn clean package
```

## 功能特性

### ✅ 支持的功能

1. **标准 shell 命令**
   - 所有系统命令（ls, pwd, cat, grep 等）
   - Git 命令
   - Maven/Gradle 等构建工具
   - 自定义脚本

2. **Shell 特性**
   - 管道：`!ls -la | grep java`
   - 重定向：`!echo "test" > file.txt`
   - 通配符：`!find . -name '*.java'`

3. **输出处理**
   - 标准输出（stdout）：正常显示
   - 标准错误（stderr）：红色显示
   - 退出码：非 0 时显示警告

4. **超时控制**
   - 默认超时：120 秒（2 分钟）
   - 最大超时：600 秒（10 分钟）
   - 超时自动中断

5. **输出限制**
   - 最多显示 1000 行
   - 超出部分会显示截断提示

### ❌ 安全限制

以下危险命令被禁止执行：

```bash
!rm       # 删除文件
!rmdir    # 删除目录
!del      # Windows 删除命令
!format   # 格式化
!shutdown # 关机
!reboot   # 重启
!halt     # 停机
!dd       # 磁盘复制
!mkfs     # 创建文件系统
!fdisk    # 磁盘分区
```

尝试执行这些命令会返回错误：
```
命令 'xxx' 因安全原因被禁止执行
```

## 使用场景

### 场景 1: 项目探索

```bash
# 查看项目结构
koder> !tree -L 2

# 查找特定文件
koder> !find . -name '*.java'

# 统计代码行数
koder> !find . -name '*.java' | xargs wc -l
```

### 场景 2: Git 工作流

```bash
# 查看状态
koder> !git status

# 查看提交历史
koder> !git log --oneline -10

# 查看差异
koder> !git diff

# 然后向 AI 助手提问
koder> 如何创建一个新的 feature 分支？
```

### 场景 3: 构建和测试

```bash
# Maven 构建
koder> !mvn clean package

# 运行测试
koder> !mvn test

# 查看测试结果
koder> !cat target/surefire-reports/*.txt
```

### 场景 4: 系统诊断

```bash
# 查看系统信息
koder> !uname -a

# 查看 Java 版本
koder> !java -version

# 查看进程
koder> !ps aux | grep java

# 查看端口占用
koder> !lsof -i :8080
```

### 场景 5: 与 AI 助手配合

```bash
# 步骤 1: 使用 ! 命令收集信息
koder> !git log --oneline -5
koder> !git diff HEAD~1

# 步骤 2: 向 AI 助手提问
koder> 请分析最近的提交，有什么需要改进的地方？

# 步骤 3: 根据建议执行操作
koder> !git commit --amend
```

## 实现细节

### 架构设计

```
用户输入 "!ls -la"
    ↓
REPLEngine.processInput()
    ↓
检测到 ! 前缀
    ↓
REPLEngine.executeBashCommand()
    ↓
BashTool.validateInput()  [安全检查]
    ↓
BashTool.call()           [执行命令]
    ↓
显示输出结果
```

### 核心代码

**REPLEngine.java**
```java
private boolean processInput(String input) {
    // 检查是否为Bash模式（!开头）
    if (input.startsWith("!")) {
        String command = input.substring(1).trim();
        if (!command.isEmpty()) {
            return executeBashCommand(command);
        }
        return false;
    }
    
    // 其他处理逻辑...
}

private boolean executeBashCommand(String command) {
    renderer.println("! " + command);
    
    BashTool.Input input = BashTool.Input.builder()
            .command(command)
            .build();
    
    bashTool.call(input, null)
            .doOnNext(response -> {
                BashTool.Output output = response.getData();
                // 显示输出...
            })
            .blockLast();
    
    return false;
}
```

**BashTool.java**
```java
@Override
public ValidationResult validateInput(Input input, ToolUseContext context) {
    String command = input.command.trim();
    String baseCmd = command.split("\\s+")[0];
    
    if (BANNED_COMMANDS.contains(baseCmd)) {
        return ValidationResult.failure(
            String.format("命令 '%s' 因安全原因被禁止执行", baseCmd)
        );
    }
    
    return ValidationResult.success();
}
```

## 常见问题

### Q1: 如何执行需要交互的命令？
**A**: Bash 模式不支持交互式命令。建议使用非交互参数，如：
```bash
# 错误：需要交互
koder> !vim file.txt

# 正确：使用 cat 查看
koder> !cat file.txt
```

### Q2: 如何设置命令超时时间？
**A**: 目前使用默认超时（120秒）。如需更长时间，可以通过工具参数设置（开发中）。

### Q3: 输出被截断怎么办？
**A**: 可以使用 head/tail 命令限制输出：
```bash
koder> !find . -name '*.java' | head -100
```

### Q4: 如何执行多个命令？
**A**: 使用分号或管道连接：
```bash
koder> !cd src && ls -la
koder> !ls | grep java | wc -l
```

### Q5: 为什么某些命令被禁止？
**A**: 出于安全考虑，危险的系统操作命令被列入黑名单。如需执行，请直接在系统终端中操作。

## 最佳实践

### ✅ 推荐做法

1. **快速信息收集**
   ```bash
   koder> !git status
   koder> !mvn -version
   ```

2. **结合 AI 分析**
   ```bash
   koder> !cat error.log
   koder> 请帮我分析这个错误
   ```

3. **验证操作结果**
   ```bash
   koder> 创建一个新的 Java 类
   koder> !ls src/main/java/
   ```

### ❌ 避免做法

1. **不要执行危险命令**
   ```bash
   # 错误
   koder> !rm -rf *
   ```

2. **不要执行长时间运行的命令**
   ```bash
   # 可能超时
   koder> !find / -name '*.log'
   ```

3. **不要依赖交互式输入**
   ```bash
   # 无法工作
   koder> !ssh user@server
   ```

## 进阶技巧

### 1. 命令组合

```bash
# 查找并统计
koder> !find . -name '*.java' | wc -l

# 过滤和排序
koder> !ls -la | grep java | sort -k5 -n
```

### 2. 临时脚本

```bash
# 创建并执行脚本
koder> !echo 'ls -la' > temp.sh && chmod +x temp.sh && ./temp.sh
```

### 3. 环境检查

```bash
# 一键检查开发环境
koder> !java -version && mvn -version && git --version
```

## 总结

Koder 的 Bash 模式提供了以下优势：

- ✅ **无缝集成**：无需切换模式，直接在对话中执行
- ✅ **安全可靠**：内置安全限制，防止误操作
- ✅ **功能完整**：支持管道、重定向等 shell 特性
- ✅ **智能配合**：与 AI 助手完美结合

开始使用：
```bash
cd Koder
mvn clean package
java -jar koder-cli/target/koder.jar
```

在 REPL 中输入 `!` 前缀即可开始使用！
