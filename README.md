# Koder - 智能终端 AI 助手

<div align="center">

一个基于 Java 的强大终端 AI 助手，支持多模型接入、智能代理、CLI 交互与 MCP 协议集成。

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)

</div>

## ✨ 核心特性

- 🤖 **多 LLM 支持** - 支持 DeepSeek、通义千问(Qwen)、Ollama 等多种大语言模型
- 🛠️ **智能工具系统** - 内置近 20 种工具，包括文件操作、命令执行、Web 搜索等
- 🎯 **代理系统** - 内置 7 个专用智能代理，支持任务驱动执行
- 🔌 **MCP 集成** - 支持 Model Context Protocol 协议扩展上下文能力
- 💬 **流式响应** - 实时输出 LLM 推理结果
- 🖥️ **REPL 交互** - 友好的命令行交互界面

## 📦 项目结构

```
koder/
├── koder-core      # 核心配置、权限、上下文、消息系统
├── koder-tools     # 内置工具实现（文件操作、命令执行、搜索等）
├── koder-cli       # REPL 命令系统与终端交互入口
├── koder-mcp       # Model Context Protocol 客户端与服务集成
├── koder-agent     # 智能代理加载与执行系统
├── scripts/        # 构建与运行脚本
└── doc/            # 项目文档
```

## 🚀 快速开始

### 环境要求

- **JDK**: 17 或更高版本
- **Maven**: 3.9 或更高版本
- **操作系统**: macOS / Linux / Windows

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd Koder
```

2. **设置 JAVA_HOME** (如果未设置)
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

3. **编译项目**
```bash
mvn clean compile -DskipTests
```

### 运行方式

#### 方式 1: 使用快速启动脚本 (推荐)
```bash
./run-koder.sh
```

#### 方式 2: 构建 JAR 后运行
```bash
./build-jar.sh
java -jar koder-cli/target/koder.jar
```

#### 方式 3: 使用 Maven 直接运行
```bash
mvn spring-boot:run -pl koder-cli -DskipTests
```

#### 方式 4: 全局安装
```bash
./install.sh
koder  # 在任意目录下运行
```

## 🔧 配置说明

### LLM 提供商配置

在 `application.yml` 或 `application-llm.yml` 中配置：

```yaml
llm:
  providers:
    deepseek:
      enabled: true
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      models:
        - name: deepseek-chat
          alias: deepseek
    
    qwen:
      enabled: true
      api-key: ${QWEN_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      models:
        - name: qwen-max
          alias: qwen
    
    ollama:
      enabled: true
      base-url: http://localhost:11434
      models:
        - name: llama2
          alias: ollama
```

### MCP 服务器配置

在 `application.yml` 中配置 MCP 服务器：

```yaml
mcp:
  servers:
    - name: filesystem
      command: npx
      args:
        - -y
        - @modelcontextprotocol/server-filesystem
        - /path/to/workspace
```

## 📚 内置工具

Koder 提供丰富的内置工具：

| 工具 | 功能描述 |
|------|---------|
| `BashTool` | 执行 Bash 命令 |
| `FileReadTool` | 读取文件内容 |
| `FileWriteTool` | 写入文件 |
| `FileEditTool` | 编辑文件 |
| `MultiEditTool` | 批量编辑多个文件 |
| `GrepTool` | 文件内容搜索 |
| `GlobTool` | 文件路径匹配 |
| `LSTool` | 列出目录内容 |
| `WebSearchTool` | Web 搜索 |
| `URLFetcherTool` | 获取 URL 内容 |
| `MemoryReadTool` | 读取记忆 |
| `MemoryWriteTool` | 写入记忆 |
| `TaskTool` | 任务管理 |
| `ThinkTool` | 思考工具 |
| `NotebookEditTool` | 笔记编辑 |
| `TodoWriteTool` | 待办事项 |

## 🤖 智能代理

内置多个专业领域的智能代理：

- **AI Engineer** - AI 工程师代理
- **Backend Architect** - 后端架构师代理
- **Senior Backend Architect** - 高级后端架构师代理

代理配置文件位于：`koder-cli/src/main/resources/.koder/agents/`

## 💡 使用示例

### 基础对话
```bash
koder> 你好，请介绍一下自己
```

### 执行命令
```bash
koder> 帮我列出当前目录的文件
```

### 文件操作
```bash
koder> 读取 README.md 文件的内容
```

### 切换模型
```bash
koder> /model deepseek
```

## 🛠️ 开发指南

### 编译项目
```bash
mvn clean compile -DskipTests
```

### 运行测试
```bash
mvn test
```

### 打包项目
```bash
mvn clean package -DskipTests
```

### 添加新工具

1. 在 `koder-tools` 模块创建新的工具类，继承 `AbstractTool`
2. 实现 `execute()` 方法
3. 在 `ToolSystemConfiguration` 中注册工具

### 添加新代理

在 `koder-cli/src/main/resources/.koder/agents/` 目录下创建新的 Markdown 配置文件。

## 📖 文档

更多详细文档请查看 `doc/` 目录：

- [用户手册](doc/USER_MANUAL.md)
- [Bash 模式指南](doc/BASH_MODE_GUIDE.md)
- [模块集成说明](doc/MODULE_INTEGRATION.md)
- [电商开发指南](doc/ECOMMERCE_DEVELOPMENT_GUIDE.md)

## 🔍 技术栈

- **Java 17** - 编程语言
- **Spring Boot 3.2+** - 应用框架
- **Spring AI 1.0.0-M4** - AI 集成
- **JLine 3.x** - 终端 UI
- **Jackson 2.17.1** - JSON 处理
- **Maven** - 构建工具
- **Logback** - 日志系统

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

## 📄 许可证

[添加您的许可证信息]

## 📧 联系方式

如有问题或建议，请联系项目维护者。

---

<div align="center">
Made with ❤️ by the Koder Team
</div>
