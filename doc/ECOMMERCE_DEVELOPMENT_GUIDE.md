# 电商网站全链路开发指南

> **基于 Koder 的端到端自动化开发实战手册**

**版本**: 1.0.0  
**适用场景**: 从零开发电商网站的完整软件开发生命周期  
**目标读者**: 技术负责人、架构师、全栈工程师  
**最后更新**: 2025-10-31

---

## 📋 目录

- [第一章 开发准备](#第一章-开发准备)
- [第二章 需求分析阶段](#第二章-需求分析阶段)
- [第三章 技术方案设计](#第三章-技术方案设计)
- [第四章 代码编写阶段](#第四章-代码编写阶段)
- [第五章 测试阶段](#第五章-测试阶段)
- [第六章 代码审查与优化](#第六章-代码审查与优化)
- [第七章 部署发布](#第七章-部署发布)
- [第八章 运维与监控](#第八章-运维与监控)
- [附录](#附录)

---

## 第一章 开发准备

### 1.1 环境配置与 Koder 安装

#### 1.1.1 系统要求

```bash
# 必备软件
- Java: JDK 17 或更高版本
- Maven: 3.9+
- Git: 2.30+
- Node.js: 20.18.1+ (如果开发前端)
- Docker: 20.10+ (用于容器化部署)

# 推荐配置
- 内存: 8GB+
- 硬盘: 50GB+ 可用空间
- 操作系统: macOS、Linux 或 Windows (WSL)
```

#### 1.1.2 安装 Koder

**快速安装（推荐）**

```bash
# 1. 克隆 Koder 项目
cd ~/workspace
git clone https://github.com/your-org/kode.git
cd kode/Koder

# 2. 构建可执行 JAR
./scripts/build-jar.sh

# 3. 全局安装（可选）
./scripts/install.sh

# 4. 验证安装
koder --version
```

#### 1.1.3 配置 AI 模型

创建或编辑 `~/.koder.json` 配置文件：

```json
{
  "modelProfiles": [
    {
      "name": "gpt-4-architect",
      "provider": "openai",
      "modelName": "gpt-4-turbo",
      "apiKey": "sk-your-api-key-here",
      "maxTokens": 8192,
      "contextLength": 128000,
      "isActive": true
    },
    {
      "name": "gpt-5-coder",
      "provider": "openai",
      "modelName": "gpt-5",
      "apiKey": "sk-your-gpt5-api-key",
      "maxTokens": 16384,
      "contextLength": 200000,
      "isActive": true
    },
    {
      "name": "claude-reviewer",
      "provider": "anthropic",
      "modelName": "claude-3-5-sonnet-20241022",
      "apiKey": "sk-ant-your-key",
      "maxTokens": 4096,
      "contextLength": 200000,
      "isActive": true
    }
  ],
  "modelPointers": {
    "main": "gpt-4-architect",
    "task": "gpt-5-coder",
    "reasoning": "claude-reviewer",
    "quick": "gpt-4-architect"
  }
}
```

**模型选择建议**：
- **架构设计**: GPT-4 Turbo 或 Claude 3.5 Sonnet（推理能力强）
- **代码编写**: GPT-5（代码生成质量高，支持持久化记忆）
- **代码审查**: Claude 3.5 Sonnet（审查严谨，安全意识强）
- **测试编写**: GPT-4 或 DeepSeek Coder（测试覆盖率高）

#### 1.1.4 启动 Koder

```bash
# 方式1: 使用全局命令
koder

# 方式2: 使用 JAR 文件
java -jar ~/workspace/kode/Koder/koder-cli/target/koder.jar

# 方式3: 使用快捷脚本
cd ~/workspace/kode/Koder
./scripts/run-koder.sh
```

---

### 1.2 需求文档准备

#### 1.2.1 需求文档结构（xuqiu.md 示例）

假设您已有需求文档 `xuqiu.md`，典型电商网站需求包括：

- 用户模块（注册/登录/个人信息）
- 商品模块（分类/列表/详情/搜索）
- 购物车模块（增删改查）
- 订单模块（创建/支付/状态管理）
- 支付模块（支付宝/微信）
- 管理后台（商家/运营）

#### 1.2.2 在 Koder 中加载需求文档

```bash
koder> 请读取并分析需求文档 xuqiu.md，理解整体需求
```

---

### 1.3 项目初始化

#### 1.3.1 创建工作目录

```bash
koder> 请在 ~/workspace 下创建电商项目目录结构：
ecommerce/
├── backend/          # 后端代码
├── frontend/         # 前端代码
├── docs/             # 文档
├── scripts/          # 脚本
└── docker/           # Docker配置

并初始化 Git 仓库
```

#### 1.3.2 配置项目级 Koder 配置

```bash
koder> 在 ~/workspace/ecommerce/.koder.json 中创建项目配置
```

生成的配置示例：

```json
{
  "projectName": "ecommerce-platform",
  "language": "java",
  "framework": "spring-boot",
  "codeStyle": "alibaba",
  "testFramework": "junit5",
  "frontend": {
    "framework": "react",
    "language": "typescript"
  }
}
```

---

## 第二章 需求分析阶段

### 2.1 需求理解与拆解

#### 2.1.1 调用 Architect Agent 分析需求

```bash
koder> /agents run architect

architect> 分析 xuqiu.md 中的需求，生成：
1. 功能模块清单
2. 模块优先级排序（P0/P1/P2）
3. 模块依赖关系图
4. 开发时间估算
```

**Architect Agent 输出示例**：

```markdown
## 功能模块分析

### 核心模块（P0 - 第一期必须完成）
1. **用户模块** (5人日)
2. **商品模块** (8人日)
3. **购物车模块** (3人日)
4. **订单模块** (10人日)

### 重要模块（P1 - 第二期）
5. **支付模块** (7人日)
6. **商家后台** (12人日)

### 增强模块（P2 - 第三期）
7. **评价系统** (5人日)
8. **数据报表** (6人日)
```

#### 2.1.2 生成任务列表

```bash
koder> 基于上述分析，创建详细任务列表
```

---

### 2.2 业务建模

#### 2.2.1 领域模型设计

```bash
koder> 基于电商需求，设计领域模型（Domain Model）：
- 识别核心实体（Entity）
- 定义值对象（Value Object）
- 建立聚合关系（Aggregate）
```

**Koder 输出**：

```markdown
## 领域模型

### 核心实体
1. **User（用户）**
2. **Product（商品）**
3. **Order（订单）**
4. **Cart（购物车）**

### 值对象
- Address
- Money
- OrderStatus
```

---

### 2.3 非功能需求分析

#### 2.3.1 性能需求量化

```bash
koder> 分析非功能需求中的性能指标，给出具体的技术方案：
- 10万并发如何实现
- 200ms响应时间优化策略
```

---

## 第三章 技术方案设计

### 3.1 架构设计

#### 3.1.1 总体架构设计

```bash
koder> /agents run architect

architect> 设计电商平台的总体架构，包括：
1. 分层架构（前端、网关、微服务、数据层）
2. 技术组件选型
3. 部署架构图
```

**架构风格**: 微服务 + 前后端分离

---

### 3.2 技术选型

| 技术领域 | 推荐方案 | 理由 |
|---------|---------|------|
| **后端框架** | **Spring Boot 3.2** | 生态成熟、团队熟悉 |
| **ORM框架** | **MyBatis-Plus** | 灵活、性能好 |
| **缓存** | **Redis** | 支持数据结构、持久化 |
| **消息队列** | **RabbitMQ** | 适合业务消息 |
| **前端框架** | **React 18** | 性能优、TypeScript支持好 |

---

### 3.3 数据库设计

#### 3.3.1 表结构设计

```bash
koder> 设计电商核心表结构（用户、商品、订单、购物车），生成 SQL DDL
```

**Koder 生成 SQL**：

```sql
-- 用户表
CREATE TABLE `user` (
  `user_id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `phone` VARCHAR(20) UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE `product` (
  `product_id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `stock` INT DEFAULT 0,
  `category_id` BIGINT NOT NULL,
  `status` TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE `order` (
  `order_id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) UNIQUE NOT NULL,
  `user_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 购物车表
CREATE TABLE `cart` (
  `cart_id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 3.4 API 设计

```bash
koder> 设计核心业务的 RESTful API 接口，使用 OpenAPI 3.0 规范
```

---

## 第四章 代码编写阶段

### 4.1 项目骨架生成

#### 4.1.1 生成后端项目结构

```bash
koder> 在 ~/workspace/ecommerce/backend 下创建 Spring Boot 微服务项目骨架：

项目结构:
ecommerce-backend/
├── pom.xml (父POM)
├── common/ (公共模块)
├── user-service/ (用户服务)
├── product-service/ (商品服务)
├── order-service/ (订单服务)
```

---

### 4.2 核心模块开发

#### 4.2.1 用户服务开发

```bash
koder> 开发用户服务的注册和登录功能：

需求:
1. 用户注册（手机号+密码）
2. 用户登录（返回JWT Token）
3. 密码使用BCrypt加密

请生成完整代码：
- UserController
- UserService
- UserRepository
```

#### 4.2.2 商品服务开发

```bash
koder> 开发商品服务的核心功能：
1. 商品列表（分页、筛选、排序）
2. 商品详情
3. 使用 Redis 缓存热点商品
```

#### 4.2.3 订单服务开发

```bash
koder> 开发订单服务的创建订单功能：

业务流程:
1. 验证购物车商品库存
2. 计算订单总金额
3. 扣减商品库存
4. 创建订单记录
5. 发送消息到 RabbitMQ
```

---

### 4.3 前端页面开发

#### 4.3.1 创建前端项目

```bash
koder> 在 ~/workspace/ecommerce/frontend 创建 React 项目：

技术栈:
- Vite + React 18 + TypeScript
- Ant Design UI
- React Router v6

请执行 npm create vite 命令并配置项目
```

#### 4.3.2 开发商品列表页面

```bash
koder> 开发商品列表页面：

功能:
1. 商品卡片展示
2. 分类筛选
3. 搜索框
4. 分页

请生成 ProductList.tsx 组件
```

---

### 4.4 集成与联调

```bash
koder> 启动所有服务并进行联调测试：
1. 启动 MySQL 和 Redis
2. 启动后端微服务
3. 启动前端开发服务器
4. 测试完整业务流程
```

---

## 第五章 测试阶段

### 5.1 单元测试编写

#### 5.1.1 调用 Test Writer Agent

```bash
koder> /agents run test-writer

test-writer> 为 UserService 生成单元测试：
- 测试用户注册（成功/失败场景）
- 测试用户登录（成功/失败场景）
- 使用 Mockito 模拟依赖
- 测试覆盖率 > 80%
```

**Test Writer 生成代码**：

```java
@SpringBootTest
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void testRegister_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        request.setPassword("password123");
        
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        // When
        userService.register(request);
        
        // Then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_PhoneAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        
        when(userRepository.existsByPhone(anyString())).thenReturn(true);
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            userService.register(request);
        });
    }

    @Test
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setAccount("13800138000");
        request.setPassword("password123");
        
        User user = new User();
        user.setUserId(1L);
        user.setPasswordHash("hashedPassword");
        
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // When
        String token = userService.login(request);
        
        // Then
        assertNotNull(token);
    }
}
```

#### 5.1.2 批量生成测试

```bash
koder> 为以下服务批量生成单元测试：
1. ProductService
2. OrderService
3. CartService

每个服务至少包含 5 个测试用例
```

---

### 5.2 集成测试

#### 5.2.1 API 集成测试

```bash
koder> 生成用户服务的集成测试，使用 TestRestTemplate 测试完整 HTTP 请求
```

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRegisterAndLogin() {
        // 1. 注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone("13900139000");
        registerRequest.setPassword("test123");
        
        ResponseEntity<ApiResponse> registerResponse = restTemplate.postForEntity(
            "/users/register", 
            registerRequest, 
            ApiResponse.class
        );
        
        assertEquals(200, registerResponse.getStatusCode().value());
        
        // 2. 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("13900139000");
        loginRequest.setPassword("test123");
        
        ResponseEntity<ApiResponse> loginResponse = restTemplate.postForEntity(
            "/users/login", 
            loginRequest, 
            ApiResponse.class
        );
        
        assertEquals(200, loginResponse.getStatusCode().value());
        assertNotNull(loginResponse.getBody().getData());
    }
}
```

---

### 5.3 性能测试

#### 5.3.1 使用 JMeter 进行压测

```bash
koder> 生成 JMeter 测试计划，模拟以下场景：
1. 商品列表查询（1000 并发）
2. 创建订单（500 并发）
3. 持续时间 5 分钟

保存为 performance-test.jmx
```

---

## 第六章 代码审查与优化

### 6.1 代码质量检查

#### 6.1.1 调用 Code Reviewer Agent

```bash
koder> /agents run code-reviewer

code-reviewer> 审查 UserService.java，检查：
1. 代码规范（阿里巴巴 Java 规范）
2. 潜在 Bug
3. 性能问题
4. 安全漏洞
```

**Code Reviewer 输出**：

```markdown
## 代码审查报告 - UserService.java

### 🔴 严重问题
1. **SQL 注入风险** (第 45 行)
   - 问题: 直接拼接 SQL 查询
   - 建议: 使用参数化查询

### 🟡 性能优化建议
2. **缓存缺失** (第 78 行)
   - 问题: 频繁查询数据库获取用户信息
   - 建议: 添加 @Cacheable 注解

### 🟢 代码规范
3. **命名规范** (第 23 行)
   - 问题: 变量名 `u` 不符合规范
   - 建议: 改为 `user`

### 总体评分
- 安全性: 6/10
- 性能: 7/10
- 可维护性: 8/10
```

---

### 6.2 安全审计

#### 6.2.1 调用 Security Auditor Agent

```bash
koder> /agents run security-auditor

security-auditor> 对整个项目进行安全审计：
1. SQL 注入检测
2. XSS 漏洞扫描
3. 敏感信息泄露检查
4. 依赖库漏洞扫描
```

---

### 6.3 性能优化

#### 6.3.1 数据库查询优化

```bash
koder> 分析慢查询日志，优化以下 SQL：

SELECT * FROM `order` WHERE user_id = 123 ORDER BY created_at DESC;

给出优化建议并生成优化后的 SQL
```

**Koder 建议**：

```markdown
## 优化方案

### 问题分析
1. 缺少索引，导致全表扫描
2. SELECT * 查询所有字段，浪费带宽

### 优化措施
1. 添加复合索引：
```sql
CREATE INDEX idx_user_created ON `order`(user_id, created_at DESC);
```

2. 只查询必要字段：
```sql
SELECT order_id, order_no, total_amount, status, created_at 
FROM `order` 
WHERE user_id = 123 
ORDER BY created_at DESC;
```

### 预期效果
- 查询时间: 500ms → 50ms
- 索引命中率: 100%
```

---

## 第七章 部署发布

### 7.1 容器化部署

#### 7.1.1 生成 Dockerfile

```bash
koder> 为用户服务生成 Dockerfile（多阶段构建）
```

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 7.1.2 生成 Docker Compose

```bash
koder> 生成 docker-compose.yml，包含所有服务（MySQL、Redis、微服务）
```

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: ecommerce
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  user-service:
    build: ./user-service
    ports:
      - "8081:8080"
    depends_on:
      - mysql
      - redis

  product-service:
    build: ./product-service
    ports:
      - "8082:8080"
    depends_on:
      - mysql
      - redis
```

---

### 7.2 CI/CD 配置

#### 7.2.1 生成 GitHub Actions 工作流

```bash
koder> 生成 GitHub Actions CI/CD 配置：
1. 代码检查（Checkstyle）
2. 单元测试
3. 构建 Docker 镜像
4. 推送到容器仓库
```

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          
      - name: Run tests
        run: mvn test
      
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker image
        run: docker build -t ecommerce-user-service .
      
      - name: Push to registry
        run: docker push ecommerce-user-service:latest
```

---

### 7.3 生产环境发布

#### 7.3.1 Kubernetes 部署

```bash
koder> 生成 Kubernetes 部署配置（Deployment + Service）
```

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: ecommerce-user-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:mysql://mysql:3306/ecommerce
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

---

## 第八章 运维与监控

### 8.1 日志系统

```bash
koder> 集成 ELK 日志系统：
1. 配置 Logback 输出 JSON 格式日志
2. 配置 Filebeat 采集日志
3. 配置 Logstash 解析日志
4. 配置 Kibana 可视化
```

---

### 8.2 监控告警

```bash
koder> 集成 Prometheus + Grafana 监控：
1. 添加 Spring Boot Actuator
2. 配置 Prometheus 抓取指标
3. 创建 Grafana Dashboard
```

---

### 8.3 故障处理

```bash
koder> /agents run bug-fixer

bug-fixer> 分析生产环境错误日志，定位问题并提供修复方案：

错误日志：
java.lang.NullPointerException at OrderService.createOrder(OrderService.java:45)
```

---

## 附录

### A. Koder 核心命令速查

| 命令 | 功能 | 示例 |
|------|------|------|
| `/help` | 显示帮助 | `/help` |
| `/model` | 模型管理 | `/model` |
| `/agents` | 代理管理 | `/agents run architect` |
| `/config` | 配置管理 | `/config` |
| `/init` | 初始化项目 | `/init` |

---

### B. 专用 Agent 使用指南

#### B.1 Architect（架构设计师）

**适用场景**: 系统架构设计、技术选型

```bash
koder> /agents run architect
architect> 设计微服务架构
```

**可用工具**: FileRead, Grep, LS, WebSearch（只读）

#### B.2 Test Writer（测试工程师）

**适用场景**: 编写单元测试、集成测试

```bash
koder> /agents run test-writer
test-writer> 为 UserService 生成单元测试
```

**可用工具**: FileRead, FileEdit, FileWrite, Bash

#### B.3 Code Reviewer（代码审查员）

**适用场景**: 代码审查、质量检查

```bash
koder> /agents run code-reviewer
code-reviewer> 审查 UserController.java
```

**可用工具**: FileRead, Grep（只读）

#### B.4 Bug Fixer（问题修复专家）

**适用场景**: Bug 定位与修复

```bash
koder> /agents run bug-fixer
bug-fixer> 修复 NullPointerException 错误
```

**可用工具**: FileRead, FileEdit, Grep, Bash

#### B.5 Security Auditor（安全审计员）

**适用场景**: 安全漏洞扫描

```bash
koder> /agents run security-auditor
security-auditor> 扫描 SQL 注入漏洞
```

**可用工具**: FileRead, Grep, WebSearch（只读）

---

### C. 常见问题与解决方案

#### C.1 Koder 启动失败

**问题**: `Exception in thread "main" java.lang.UnsupportedClassVersionError`

**原因**: Java 版本低于 17

**解决方案**:
```bash
# 检查 Java 版本
java -version

# 安装 JDK 17
brew install openjdk@17  # macOS
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

#### C.2 模型调用失败

**问题**: `API key invalid`

**解决方案**:
```bash
# 检查配置文件
cat ~/.koder.json

# 验证 API Key
curl https://api.openai.com/v1/models -H "Authorization: Bearer YOUR_API_KEY"
```

#### C.3 权限被拒绝

**问题**: Koder 无法执行文件操作

**解决方案**:
```bash
# 启用安全模式，手动授权
koder --safe
```

---

## 总结

本指南详细介绍了如何使用 Koder 完成电商网站的全链路开发，涵盖从需求分析到部署发布的完整流程。

**核心流程**:

1. **准备阶段**: 安装 Koder、配置模型、准备需求文档
2. **需求分析**: 使用 Architect Agent 分析需求、拆解任务
3. **方案设计**: 设计架构、选型技术、设计数据库和 API
4. **代码编写**: 生成项目骨架、开发核心模块、前后端联调
5. **测试阶段**: 使用 Test Writer 生成单元测试、集成测试
6. **代码审查**: 使用 Code Reviewer 和 Security Auditor 检查代码
7. **部署发布**: 容器化部署、配置 CI/CD、发布到生产环境
8. **运维监控**: 日志收集、监控告警、故障处理

**最佳实践**:

- ✅ 使用专用 Agent 处理特定任务（架构设计、测试、审查）
- ✅ 充分利用 Koder 的多模型协作能力
- ✅ 遵循代码规范，保持一致的开发风格
- ✅ 编写高覆盖率的单元测试
- ✅ 定期进行代码审查和安全审计
- ✅ 自动化部署流程，减少人工干预

**下一步行动**:

1. 根据实际需求文档（xuqiu.md）启动项目
2. 使用 Koder 的 `/agents run architect` 进行需求分析
3. 按照本指南逐步完成开发任务
4. 持续迭代优化，交付高质量的电商平台

---

**文档维护**: 本文档将随 Koder 版本更新持续维护，请关注最新版本。

**反馈渠道**: 如有问题或建议，请提交 Issue 到 Koder 项目仓库。
