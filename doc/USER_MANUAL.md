# Koder 用户使用手册

> **完整研发流程指南**：从需求到提测的全流程AI辅助开发

**版本**: 1.0.0  
**适用对象**: 软件开发工程师、技术负责人、产品经理  
**最后更新**: 2025-10-31

---

## 目录

- [第一章 快速上手](#第一章-快速上手)
- [第二章 系统分析阶段](#第二章-系统分析阶段)
- [第三章 技术方案设计](#第三章-技术方案设计)
- [第四章 编码实现阶段](#第四章-编码实现阶段)
- [第五章 单元测试阶段](#第五章-单元测试阶段)
- [第六章 代码审查阶段](#第六章-代码审查阶段)
- [第七章 提测准备](#第七章-提测准备)
- [第八章 智能代理系统](#第八章-智能代理系统)
- [第九章 工具系统详解](#第九章-工具系统详解)
- [第十章 高级功能](#第十章-高级功能)
- [附录](#附录)

---

## 第一章 快速上手

### 1.1 安装与配置

#### 1.1.1 系统要求

- **Java**: JDK 17 或更高版本
- **Maven**: 3.9+ (用于构建)
- **操作系统**: macOS、Linux 或 Windows (需 WSL/Git Bash)
- **内存**: 建议 4GB 以上可用内存

#### 1.1.2 环境准备

```bash
# 1. 检查 Java 版本
java -version  # 应显示 17 或更高

# 2. 设置 JAVA_HOME (如果未设置)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 3. 验证 Maven
mvn -version
```

#### 1.1.3 安装步骤

**方式一：可执行 JAR (推荐，开箱即用)**

```bash
# 进入项目目录
cd Koder

# 构建可执行 JAR
./scripts/build-jar.sh

# 运行
java -jar koder-cli/target/koder.jar

# 或使用快捷脚本
./scripts/run-jar.sh
```

**方式二：系统级安装 (全局命令)**

```bash
# 打包并安装
./scripts/build-jar.sh
./scripts/install.sh

# 在任何目录下运行
koder
```

**方式三：开发模式 (源码运行)**

```bash
# 编译项目
mvn clean compile -DskipTests

# 使用 Maven 运行
mvn spring-boot:run -pl koder-cli -DskipTests
```

#### 1.1.4 配置 AI 模型

首次运行 Koder 时，需要配置 AI 模型。手动配置编辑 `~/.koder.json`：

```json
{
  "modelProfiles": [
    {
      "name": "gpt-4",
      "provider": "openai",
      "modelName": "gpt-4-turbo",
      "apiKey": "your-api-key-here",
      "maxTokens": 4096,
      "contextLength": 128000,
      "isActive": true
    }
  ],
  "modelPointers": {
    "main": "gpt-4",
    "task": "gpt-4",
    "reasoning": "gpt-4",
    "quick": "gpt-4"
  }
}
```

### 1.2 首次运行

启动 Koder 后界面：

```
╔══════════════════════════════════════════╗
║        Koder - AI编程助手 (Java版)      ║
╚══════════════════════════════════════════╝

输入 /help 查看帮助
输入 /exit 退出程序

koder> 
```

### 1.3 基础命令

| 命令 | 功能 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/model` | 查看或切换模型 | `/model` |
| `/config` | 配置管理 | `/config` |
| `/agents` | 代理管理 | `/agents` |
| `/init` | 初始化项目上下文 | `/init` |
| `/exit` | 退出程序 | `/exit` |

---

## 第二章 系统分析阶段

### 2.1 需求理解与分析

#### 2.1.1 需求文档分析

假设您有需求文档 `requirements.md`：

```bash
koder> 请分析需求文档的内容，提取核心功能点和技术要求

需求：开发用户管理系统，包含：
- 用户注册、登录、注销
- 个人信息管理
- 权限管理（管理员、普通用户）
- 操作日志记录
```

#### 2.1.2 需求拆解与任务排序

```bash
koder> 基于以下需求，帮我拆解成可执行的任务：

【需求】
开发用户管理系统，包含用户注册、登录、权限管理和日志记录

请按优先级排序，并给出开发顺序建议。
```

Koder 会输出详细的任务拆解，按优先级分为 P0（核心基础）、P1（功能完善）、P2（辅助功能）。

#### 2.1.3 技术可行性分析

```bash
koder> 分析以下需求的技术可行性：
- 需要支持 10 万并发用户
- 响应时间 < 200ms
- 数据高可用（99.99%）

请给出技术选型建议和潜在风险。
```

### 2.2 项目初始化

#### 2.2.1 创建项目骨架

```bash
koder> 请为用户管理系统创建 Spring Boot 3.x 项目骨架：
- 分层架构（Controller、Service、Repository）
- 统一异常处理
- 统一响应格式
- 使用 Maven 作为构建工具
```

#### 2.2.2 设置项目配置

创建 `.koder.json` 项目配置：

```json
{
  "allowedTools": [
    "FileRead", "FileEdit", "FileWrite",
    "Bash", "Grep", "Glob", "LS"
  ],
  "contextFiles": [
    "README.md",
    "pom.xml"
  ],
  "hasTrustDialogAccepted": true
}
```

### 2.3 架构分析

```bash
koder> 分析当前项目结构，生成架构文档，包括：
1. 系统架构图
2. 模块依赖关系
3. 技术栈选型
4. 改进建议
```

---

## 第三章 技术方案设计

### 3.1 架构设计

#### 3.1.1 整体架构设计

```bash
koder> 设计用户管理系统的整体架构

要求：
- 采用分层架构
- 支持水平扩展
- 前后端分离
- 数据库读写分离
```

#### 3.1.2 架构文档生成

```bash
koder> 请将架构方案整理成文档，保存为 ARCHITECTURE.md

内容包括：
- 架构概述
- 技术选型及理由
- 模块划分与职责
- 接口设计规范
- 部署架构
```

### 3.2 模块设计

#### 3.2.1 模块接口定义

```bash
koder> 为用户服务定义 RESTful API 接口：

资源：User
操作：创建、查询、更新、删除

请遵循 RESTful 规范，生成接口文档。
```

#### 3.2.2 接口代码生成

```bash
koder> 根据以下接口定义生成完整代码：

【接口】用户注册
- URL: POST /api/v1/users/register
- 请求参数: username, password, email
- 校验规则: username 4-20字符，password 8-20字符
- 响应: 用户信息（脱敏）

请生成 Controller、Service、DTO、VO 类
```

### 3.3 数据库设计

#### 3.3.1 ER 图设计

```bash
koder> 为用户管理系统设计数据库：

实体：用户、角色、权限、操作日志

关系：
- 用户-角色：多对多
- 角色-权限：多对多

请输出 ER 图和详细字段说明
```

#### 3.3.2 DDL 生成

```bash
koder> 生成 MySQL DDL 语句：

要求：
- InnoDB 引擎
- UTF8MB4 字符集
- 添加必要的索引
- 包含创建时间、更新时间字段

保存为 schema.sql
```

---

## 第四章 编码实现阶段

### 4.1 代码生成

#### 4.1.1 完整模块生成

```bash
koder> 实现用户注册功能

需求：
1. 用户注册接口（POST /api/v1/users/register）
2. 参数校验（用户名、密码、邮箱）
3. 密码加密（BCrypt）
4. 检查用户名/邮箱是否已存在
5. 返回用户信息（脱敏）

请生成完整代码：Controller、Service、Mapper、Entity、DTO、VO
```

#### 4.1.2 工具类生成

```bash
koder> 生成以下工具类：

1. JwtUtil - JWT 工具类
   - 生成 Token
   - 解析 Token
   - 验证 Token

2. PasswordUtil - 密码工具类
   - 密码加密
   - 密码验证

每个类都需要完整的注释
```

### 4.2 文件操作

#### 4.2.1 创建新文件

```bash
koder> 创建文件 src/main/java/com/example/utils/RedisUtil.java

内容：Redis 工具类
功能：字符串操作、Hash 操作、设置过期时间

请生成完整代码。
```

#### 4.2.2 编辑已有文件

```bash
koder> 编辑 UserService.java 文件：

在 register 方法中添加：
1. 用户名重复性检查
2. 邮箱重复性检查
3. 事务控制

请保留原有代码结构，只修改必要部分。
```

### 4.3 代码重构

#### 4.3.1 代码优化

```bash
koder> 优化以下代码：

文件：UserController.java

优化方向：
- 性能优化（减少数据库查询）
- 代码简化（使用 Stream API）
- 安全优化（防止 SQL 注入）
```

#### 4.3.2 设计模式应用

```bash
koder> 使用设计模式重构代码：

【当前实现】
根据不同用户类型执行不同逻辑（大量 if-else）

【要求】
使用策略模式重构，使代码易于扩展。
```

### 4.4 调试与问题修复

#### 4.4.1 Bug 分析

```bash
koder> 修复以下问题：

【问题】用户登录接口偶发性返回 500 错误

【错误日志】
java.lang.NullPointerException at UserServiceImpl.login

请分析原因并修复
```

#### 4.4.2 性能问题诊断

```bash
koder> 诊断性能问题：

【现象】查询用户列表接口响应时间超过 3 秒

请识别性能瓶颈并给出优化方案
```

---

## 第五章 单元测试阶段

### 5.1 测试用例设计

```bash
koder> 为 UserService.register 方法设计测试用例

要求：
- 正常场景测试
- 边界条件测试
- 异常场景测试

请列出完整的测试用例清单。
```

### 5.2 测试代码生成

#### 5.2.1 单元测试生成

```bash
koder> 为 UserService 生成完整的单元测试

要求：
- 使用 JUnit 5
- 使用 Mockito 进行 Mock
- 覆盖所有公共方法
- 测试覆盖率 > 85%

文件保存为：src/test/java/com/example/service/UserServiceTest.java
```

#### 5.2.2 集成测试生成

```bash
koder> 生成 UserController 的集成测试：

要求：
- 使用 @SpringBootTest
- 测试完整的请求-响应流程
- 包含数据库初始化和清理

文件：UserControllerIntegrationTest.java
```

### 5.3 测试执行

```bash
koder> 执行所有单元测试：

mvn test

请监控测试执行，如果有失败的测试，帮我分析原因。
```

---

## 第六章 代码审查阶段

### 6.1 代码质量检查

```bash
koder> 审查 UserService.java

审查维度：
- 代码规范
- 设计模式使用
- 异常处理
- 日志记录
- 性能优化
- 安全问题

请给出详细的审查报告和改进建议。
```

### 6.2 安全审计

```bash
koder> 审计项目安全性

审计范围：
- SQL 注入风险
- XSS 漏洞
- CSRF 防护
- 敏感信息泄露
- 权限控制缺陷

请生成安全审计报告。
```

### 6.3 性能优化

```bash
koder> 分析项目性能瓶颈：

扫描代码库：
- 识别慢查询
- 检查循环中的 I/O 操作
- 分析缓存使用情况

生成性能优化建议报告。
```

---

## 第七章 提测准备

### 7.1 集成测试

```bash
koder> 生成端到端测试场景：

场景：用户注册到登录
步骤：注册 -> 邮箱验证 -> 登录 -> 获取信息

使用 RestAssured 编写测试代码。
```

### 7.2 文档生成

#### 7.2.1 API 文档

```bash
koder> 生成完整的 API 文档：

格式：Markdown
包括：接口概览、详细说明、示例、错误码

保存为：API_DOCUMENTATION.md
```

#### 7.2.2 部署文档

```bash
koder> 生成部署文档：

包含：环境要求、部署步骤、配置说明、启动脚本

保存为：DEPLOYMENT.md
```

### 7.3 提测清单

```bash
koder> 生成提测检查清单：

项目：用户管理系统
版本：v1.0.0

检查项：
□ 代码审查通过
□ 单元测试覆盖率 > 85%
□ 集成测试通过
□ 安全扫描无高危问题
□ API 文档完整

保存为：RELEASE_CHECKLIST.md
```

---

## 第八章 智能代理系统

### 8.1 内置专用代理

Koder 内置了 7 个专业化代理：

#### 8.1.1 Architect（架构师代理）

```bash
koder> /agents info architect

# 调用代理
koder> 设计微服务架构的电商系统

要求：支持百万级并发、高可用
```

#### 8.1.2 TestWriter（测试编写代理）

```bash
koder> 为 OrderService 生成完整测试

要求：JUnit 5 + Mockito，覆盖率 > 90%
```

#### 8.1.3 CodeReviewer（代码审查代理）

```bash
koder> 审查 PaymentController.java

重点：支付安全、异常处理、日志记录
```

#### 8.1.4 BugFixer（问题修复代理）

```bash
koder> 修复订单金额计算错误

问题：优惠券叠加使用时金额计算错误
```

#### 8.1.5 RefactorSpecialist（重构专家代理）

```bash
koder> 重构 OrderService

问题：方法过长、职责不清晰
目标：单一职责、提高可测试性
```

#### 8.1.6 DocWriter（文档撰写代理）

```bash
koder> 生成订单模块的设计文档

包含：模块概述、业务流程图、类图、接口说明
```

#### 8.1.7 SecurityAuditor（安全审计代理）

```bash
koder> 审计支付模块

审计范围：SQL 注入、XSS、权限控制
```

### 8.2 自定义代理

在项目根目录创建 `.agents/` 目录：

```bash
koder> 创建自定义代理：

文件：.agents/database-expert.md
内容：数据库专家代理

请生成代理配置模板。
```

---

## 第九章 工具系统详解

### 9.1 文件操作工具

#### FileReadTool（文件读取）

```bash
# 读取整个文件
koder> 读取 UserService.java 的内容

# 读取指定行范围
koder> 读取 UserService.java 的第 50-100 行
```

#### FileEditTool（文件编辑）

```bash
koder> 编辑 UserService.java：

在 register 方法添加参数校验
```

#### FileWriteTool（文件创建）

```bash
koder> 创建文件 RedisUtil.java

内容：Redis 工具类
```

#### MultiEditTool（批量编辑）

```bash
koder> 批量修改 Controller 层代码：

文件：UserController、RoleController
修改：添加日志记录、统一异常处理
```

### 9.2 代码搜索工具

#### GrepTool（正则搜索）

```bash
koder> 搜索所有 TODO 注释

正则表达式：TODO.*
```

#### GlobTool（文件名匹配）

```bash
koder> 查找所有 Controller 类

模式：**/*Controller.java
```

#### LSTool（目录列表）

```bash
koder> 列出 service 包下的所有文件
```

### 9.3 系统命令工具

#### BashTool（Shell 命令）

```bash
koder> 执行 Maven 编译：

mvn clean compile
```

### 9.4 AI 协作工具

#### TaskTool（子任务代理）

```bash
koder> 创建子任务：编写用户注册接口的单元测试

使用不同的模型处理子任务
```

#### ThinkTool（深度推理）

```bash
koder> 深度分析系统架构的合理性

使用推理模型进行分析
```

### 9.5 MCP 集成工具

```bash
# 查看 MCP 服务器
koder> /mcp list

# 调用 MCP 工具
koder> 使用 MCP 工具查询数据库
```

---

## 第十章 高级功能

### 10.1 多模型协作

```bash
# 切换模型
koder> /model use gpt-4

# 查看模型状态
koder> /model
```

### 10.2 记忆系统

```bash
# 写入记忆
koder> 记住：项目使用 Spring Boot 3.2

# 读取记忆
koder> 回忆一下项目的技术栈
```

### 10.3 权限管理

```bash
# 安全模式启动
java -jar koder.jar --safe

# 授予工具权限
koder> 允许使用 Bash 工具执行 git 命令
```

---

## 附录

### A. 命令速查表

| 命令 | 功能 |
|------|------|
| `/help` | 显示帮助 |
| `/model` | 模型管理 |
| `/config` | 配置管理 |
| `/agents` | 代理管理 |
| `/init` | 初始化项目 |
| `/exit` | 退出程序 |

### B. 配置文件详解

#### 全局配置 (~/.koder.json)

```json
{
  "modelProfiles": [...],
  "modelPointers": {...}
}
```

#### 项目配置 (.koder.json)

```json
{
  "allowedTools": [...],
  "contextFiles": [...]
}
```

### C. 常见问题解答

**Q: 如何切换 AI 模型？**
```bash
koder> /model use gpt-4
```

**Q: 如何清除对话历史？**
```bash
koder> /clear
```

**Q: 如何授予工具权限？**
在安全模式下，首次使用工具时会提示授权。

### D. 最佳实践

1. **项目初始化**：使用 `/init` 建立上下文
2. **代理使用**：根据任务类型选择合适的代理
3. **测试先行**：先生成测试用例，再实现代码
4. **代码审查**：定期使用 CodeReviewer 审查代码
5. **安全第一**：使用 SecurityAuditor 进行安全审计

---

## 总结

Koder 是一个功能强大的 AI 编程助手，支持完整的软件开发流程：

- ✅ **系统分析**：需求理解、任务拆解、可行性分析
- ✅ **方案设计**：架构设计、模块设计、数据库设计
- ✅ **编码实现**：代码生成、文件操作、代码重构
- ✅ **单元测试**：测试用例设计、测试代码生成
- ✅ **代码审查**：质量检查、安全审计、性能优化
- ✅ **提测准备**：集成测试、文档生成、提测清单

通过智能代理系统和丰富的工具集，Koder 可以帮助开发者显著提升开发效率，保证代码质量。

---

**© 2025 Koder Team. All rights reserved.**
