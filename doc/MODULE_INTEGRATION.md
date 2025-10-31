# Koder模块集成说明

## 📦 模块架构

Koder采用多模块Maven架构，各模块职责清晰：

```
koder-parent (父POM)
├── koder-core         (核心配置、消息模型、成本追踪)
├── koder-models       (AI模型适配器：Anthropic、OpenAI、Gemini、Qwen、DeepSeek)
├── koder-tools        (工具系统：文件操作、Shell执行、搜索等)
├── koder-mcp          (MCP客户端：支持stdio和SSE传输)
├── koder-agent        (智能代理：加载、注册、执行)
└── koder-cli          (命令行界面：REPL引擎、命令系统)
```

## 🔗 模块依赖关系

```
koder-cli
  ├─→ koder-core
  ├─→ koder-models
  ├─→ koder-tools
  ├─→ koder-mcp
  └─→ koder-agent
      └─→ koder-tools
          └─→ koder-core

koder-models → koder-core
koder-mcp → koder-core
```

## ⚙️ 集成机制

### 1. Spring Bean自动配置

各模块通过Spring Boot自动配置实现集成：

- **ToolSystemConfiguration** (`koder-tools/config/`)
  - 自动扫描所有`Tool<?, ?>`实现
  - 注册到`ToolExecutor`

- **AgentSystemConfiguration** (`koder-agent/config/`)
  - 创建`AgentLoader`、`AgentRegistry`、`AgentExecutor`
  - 自动注入`ToolExecutor`依赖

- **MCPSystemConfiguration** (`koder-mcp/config/`)
  - 创建`MCPClientManager`
  - 配置WebClient和ObjectMapper

- **ModuleIntegrationInitializer** (`koder-cli/config/`)
  - Order(1)优先级，最先执行
  - 初始化各子系统
  - 验证集成完整性

### 2. 组件扫描

`KoderCliApplication`配置了包扫描：

```java
@SpringBootApplication(scanBasePackages = {
    "io.leavesfly.koder.core",
    "io.leavesfly.koder.model",
    "io.leavesfly.koder.tool",
    "io.leavesfly.koder.mcp",
    "io.leavesfly.koder.agent",
    "io.leavesfly.koder.cli"
})
```

### 3. 工具注册流程

```
启动 → ToolSystemConfiguration
     → 扫描所有@Component的Tool实现
     → ToolExecutor.registerTool()
     → 工具可用于AI调用
```

### 4. 代理加载流程

```
启动 → AgentSystemConfiguration
     → AgentLoader从文件系统加载.md文件
     → 解析YAML frontmatter + Markdown
     → AgentRegistry缓存所有代理
     → AgentExecutor准备执行环境
```

### 5. MCP集成流程

```
启动 → MCPSystemConfiguration
     → MCPClientManager初始化
     → 读取GlobalConfig.mcpServers
     → 按需创建SSE/Stdio客户端
     → MCPToolWrapper包装为Tool接口
```

## 🚀 启动方式

### 方式1：Maven插件（推荐）

```bash
cd /Users/yefei.yf/Qoder/Kode/Koder
./run-koder.sh
```

或直接使用Maven：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn spring-boot:run -pl koder-cli -DskipTests
```

### 方式2：编译后运行

```bash
cd /Users/yefei.yf/Qoder/Kode/Koder
./start-koder.sh
```

## 📋 启动日志解读

成功集成时会看到：

```
====================================
开始初始化Koder模块集成...
====================================
→ 初始化工具系统...
  ✓ 已注册 14 个工具
    - file_read (读取文件内容)
    - file_write (写入文件)
    - bash (执行Shell命令)
    - grep (正则搜索)
    ...

→ 初始化代理系统...
  ✓ 已加载 5 个代理
    - general-purpose (BUILT_IN)
    - code-reviewer (PROJECT)
    ...

→ 初始化MCP客户端系统...
  ✓ 已配置 2 个MCP服务器
    - filesystem
    - github

→ 初始化命令系统...
  ✓ 已注册 8 个命令
    - /help - 显示帮助
    - /exit - 退出程序
    - /model - 查看或切换AI模型
    - /agents - 管理智能代理
    ...

→ 验证模块集成...
  ✓ 所有模块集成验证通过
====================================
Koder模块集成初始化完成！
====================================
```

## 🔍 集成验证

### 检查工具注册

```java
// 在任何Spring Bean中注入
@Autowired
private ToolExecutor toolExecutor;

// 获取所有工具
List<Tool<?, ?>> tools = toolExecutor.getAllTools();
```

### 检查代理加载

```java
@Autowired
private AgentRegistry agentRegistry;

// 获取所有代理
List<AgentConfig> agents = agentRegistry.getAllAgents();
```

### 检查MCP客户端

```java
@Autowired
private MCPClientManager mcpClientManager;

// 获取所有客户端
Map<String, MCPClient> clients = mcpClientManager.getAllClients();
```

## 🛠️ 常见集成问题

### 1. 工具未注册

**问题**: ToolExecutor.getAllTools()返回空列表

**原因**: 
- 工具类未添加`@Component`注解
- 包路径不在扫描范围内
- ToolSystemConfiguration未生效

**解决**: 
```java
@Component  // 确保添加此注解
public class MyTool extends AbstractTool<Input, Output> {
    // ...
}
```

### 2. 代理加载失败

**问题**: AgentRegistry.getAllAgents()只返回内置代理

**原因**:
- .md文件格式不正确
- YAML frontmatter解析失败
- 文件权限问题

**解决**: 检查.md文件格式：
```markdown
---
name: my-agent
description: My custom agent
tools: ["*"]
---

You are my custom agent...
```

### 3. MCP客户端连接失败

**问题**: MCP服务器显示"未连接"

**原因**:
- 服务器配置错误
- stdio命令不存在
- SSE URL不可达

**解决**: 检查config.json中的mcpServers配置

### 4. 循环依赖

**问题**: 编译时提示循环依赖

**解决**: 
- koder-tools不应依赖koder-cli
- 共享功能应放在koder-core
- 使用接口解耦

## ✅ 集成测试清单

- [x] 所有模块编译成功
- [x] Spring Boot应用启动
- [x] ModuleIntegrationInitializer执行
- [x] 工具系统初始化（14个工具）
- [x] 代理系统初始化（加载.md文件）
- [x] MCP系统初始化（读取配置）
- [x] 命令系统初始化（8个命令）
- [x] REPL引擎启动
- [ ] AI查询集成测试
- [ ] 工具调用集成测试
- [ ] 代理执行集成测试
- [ ] MCP工具集成测试

## 📚 参考文档

- [Spring Boot自动配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Maven多模块项目](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Project Reactor](https://projectreactor.io/docs/core/release/reference/)
