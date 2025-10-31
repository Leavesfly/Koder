# Koder - Java重构版

基于Java 17 + Spring Boot 3.x + Maven重构的Kode终端AI助手项目。

## 项目结构

```
Koder/
├── koder-core/      # 核心模块（配置、权限、上下文、消息）
├── koder-models/    # 模型适配模块（适配器工厂、提供商实现）
├── koder-tools/     # 工具系统模块（工具接口、内置工具实现）
├── koder-cli/       # CLI交互模块（REPL、命令系统、终端渲染）
├── koder-mcp/       # MCP集成模块（客户端、传输协议）
└── koder-agent/     # 智能代理模块（加载器、执行器）
```

## 技术栈

- **Java**: 17
- **构建工具**: Maven 3.9+
- **框架**: Spring Boot 3.2+
- **AI集成**: Spring AI 1.0.0-M1
- **终端UI**: JLine 3.x
- **JSON处理**: Jackson 2.x
- **HTTP客户端**: Spring WebClient
- **日志**: SLF4J + Logback

## 核心特性

- ✅ **多模型支持**: 集成DeepSeek、通义千问(Qwen)、Ollama三种LLM
- ✅ **智能工具系统**: 提供文件操作、命令执行、搜索、LLM对话等工具
- ✅ **CLI交互**: 强大的REPL命令系统
- ✅ **MCP集成**: 支持Model Context Protocol
- ✅ **代理系统**: 内置七个专用智能代理
- ✅ **流式响应**: 支持实时流式输出

## 快速开始

### 1. 编译项目

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
cd Koder
mvn clean compile -DskipTests
```

### 2. 运行Koder

**方式一：单个JAR文件（推荐、便携）** ✨

```bash
# 首次需要打包
./build-jar.sh

# 运行
java -jar koder-cli/target/koder.jar
# 或使用快捷脚本
./run-jar.sh
```

打包后的JAR文件（~50MB）可以独立运行，无需其他依赖，可复制到任何机器使用：

```bash
# 复制JAR到目标机器
cp koder-cli/target/koder.jar ~/bin/

# 直接运行
java -jar ~/bin/koder.jar
```

**方式二：系统级安装（全局命令）** 🚀

```bash
# 打包并安装
./build-jar.sh
./install.sh

# 在任何目录下运行
koder
```

安装后，`koder`命令将可在任何目录下使用，如同系统命令。

**方式三：使用快速启动脚本（开发）**

```bash
./run-koder.sh
```

**方式四：使用Maven插件（开发）**

```bash
mvn spring-boot:run -pl koder-cli -DskipTests
```

### 3. 使用Koder

启动后进入REPL界面：

```
╔══════════════════════════════════════════╗
║        Koder - AI编程助手 (Java版)      ║
╚══════════════════════════════════════════╝

输入 /help 查看帮助
输入 /exit 退出程序

koder> 
```

可用命令：
- `/help` - 显示帮助信息
- `/llm` - 与LLM对话（支持DeepSeek、Qwen、Ollama）
- `/model` - 查看或切换AI模型
- `/agents` - 管理智能代理
- `/config` - 配置管理
- `/mcp` - MCP服务器管理
- `/exit` - 退出程序

## LLM集成快速开始

Koder已集成Spring AI框架，支持多种LLM提供商。

### 1. 配置API密钥

**DeepSeek（推荐，性价比高）**

```bash
export DEEPSEEK_API_KEY=your_api_key
```

**通义千问（中文能力强）**

```bash
export QWEN_API_KEY=your_api_key
```

**Ollama（本地部署，免费）**

```bash
# 安装Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# 拉取模型
ollama pull llama2

# 启动服务
ollama serve
```

### 2. 使用LLM功能

```bash
# 基本对话
/llm 解释一下什么是依赖注入

# 指定提供商
/llm -p qwen 请用中文回答：什么是Spring Boot

# 带系统提示词
/llm -s "你是一个Java专家" 如何优化这段代码

# 查看服务状态
/llm --status

# 健康检查
/llm --health
```

### 3. 详细文档

- [LLM快速开始](doc/LLM_QUICKSTART.md) - 5分钟快速上手指南
- [LLM集成指南](doc/LLM_INTEGRATION_GUIDE.md) - 完整配置和使用指南
- [配置示例](doc/application-llm.yml) - 详细的配置示例

## 模块集成

详细的模块集成说明请参考 [MODULE_INTEGRATION.md](./MODULE_INTEGRATION.md)

### 集成架构

```
KoderCliApplication (启动入口)
  ↓
ModuleIntegrationInitializer (Order=1)
  ├→ 初始化工具系统 (ToolExecutor)
  ├→ 初始化代理系统 (AgentRegistry)
  ├→ 初始化MCP系统 (MCPClientManager)
  ├→ 初始化LLM系统 (LlmManager)
  └→ 验证集成完整性
  ↓
REPLEngine (启动REPL)
  ├→ CommandRegistry (命令路由)
  ├→ AIQueryService (AI查询)
  └→ TerminalRenderer (终端渲染)
```

### 代理执行流程

```
用户输入 → /agents run <type> <task>
  ↓
AgentsCommand → 解析命令参数
  ↓
AgentExecutor → 代理执行器
  ├→ 1. 获取代理配置 (AgentRegistry)
  ├→ 2. 准备上下文 (ToolUseContext)
  ├→ 3. 过滤工具权限 (ToolExecutor)
  ├→ 4. 选择LLM模型 (LlmManager)
  ├→ 5. 构建LLM请求 (LlmChatRequest)
  ├→ 6. 调用LLM推理 (DeepSeek/Qwen/Ollama)
  └→ 7. 处理响应、执行工具
  ↓
返回结果 → AgentResponse
```

### 核心组件

- **ToolExecutor**: 管理所有工具，支持动态注册
- **AgentRegistry**: 加载和管理智能代理
- **MCPClientManager**: 管理MCP服务器连接
- **CommandRegistry**: 注册和路由CLI命令
- **AIQueryService**: 处理AI查询和流式响应

## 构建项目

```bash
# 编译所有模块
mvn clean install

# 运行CLI
cd koder-cli
mvn spring-boot:run

# 打包可执行JAR
mvn clean package
```

## 包结构

所有代码统一使用 `io.leavesfly.koder` 包路径：

- `io.leavesfly.koder.core` - 核心功能
- `io.leavesfly.koder.model` - 模型适配
- `io.leavesfly.koder.tool` - 工具系统
- `io.leavesfly.koder.cli` - CLI交互
- `io.leavesfly.koder.mcp` - MCP集成
- `io.leavesfly.koder.agent` - 智能代理

## 开发说明

### 环境要求

- JDK 17+
- Maven 3.9+

### IDE推荐配置

- 启用Lombok注解处理器
- 配置代码格式化为Java标准
- 启用Java 17语法支持（Record、Switch表达式等）

## 配置文件

### 全局配置

位置：`~/.koder.json`

包含模型配置、主题设置、MCP服务器等全局设置。

### 项目配置

位置：`.koder.json`

包含工具授权、上下文文件、项目级MCP配置等。

## License

Apache-2.0
