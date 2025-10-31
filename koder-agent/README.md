# Koder Agent - 智能代理执行模块

## 概述

`koder-agent` 模块实现了基于Spring AI ChatClient的智能代理执行框架，提供完整的Agent配置加载、会话管理和工具集成能力。

## 核心功能

### 1. 集成Koder工具集

`AgentExecutor` 通过 `ToolExecutor` 集成了Koder的所有工具，支持：

- **工具权限控制**: 根据AgentConfig配置过滤允许使用的工具
- **工具调用执行**: 通过ToolExecutor执行工具并获取结果
- **工具Schema生成**: 为Function Calling提供工具定义

```java
// 获取Agent允许的工具列表
List<Tool<?, ?>> allowedTools = agent.getAllowedTools();

// 获取工具Schema(用于Function Calling)
List<Map<String, Object>> schemas = agent.getToolSchemas();

// 执行工具调用
Mono<String> result = agent.executeTool("View", input, context);
```

### 2. 会话历史管理

`ConversationHistory` 类管理单个会话的对话历史：

```java
// 添加用户消息
history.addUserMessage("请帮我分析这段代码");

// 添加助手响应
history.addAssistantMessage("好的，让我分析一下...");

// 添加系统消息
history.addSystemMessage("你是一个Java专家");

// 获取所有消息
List<ChatMessage> messages = history.getMessages();

// 清除历史
history.clear();
```

**特性**:
- 线程安全的会话存储(ConcurrentHashMap)
- 自动管理消息时间戳
- 支持元数据扩展
- 会话隔离和独立管理

### 3. Agent对象转换

`AgentExecutor.buildAgent()` 方法实现从 `AgentConfig` 到可执行 `Agent` 的转换：

```java
// AgentConfig配置定义
AgentConfig config = AgentConfig.builder()
    .agentType("code-reviewer")
    .systemPrompt("你是一个专业的代码审查专家")
    .tools(Arrays.asList("View", "Grep", "Search"))
    .build();

// 自动转换为Agent实例
Agent agent = agentExecutor.buildAgent(config, context);

// 执行Agent任务
Flux<String> response = agent.execute("请审查这段代码", context);
```

**转换过程**:
1. 解析AgentConfig配置
2. 过滤允许的工具列表
3. 创建或获取会话历史
4. 构建Spring AI ChatClient实例
5. 封装为可执行的Agent对象

### 4. 基于Spring ChatClient执行

Agent类的execute方法基于Spring AI ChatClient实现流式对话：

```java
public Flux<String> execute(String userInput, ToolUseContext context) {
    return Flux.create(sink -> {
        // 添加用户消息到历史
        history.addUserMessage(userInput);

        // 使用ChatClient执行(TODO: 需要Spring AI依赖)
        // chatClient.prompt()
        //     .messages(messages)
        //     .stream()
        //     .content()
        //     .subscribe(...)
        
        // 临时实现
        String response = generateMockResponse(userInput, context);
        history.addAssistantMessage(response);
        
        sink.next(response);
        sink.complete();
    });
}
```

## 核心类说明

### AgentExecutor

主要的Agent执行器类，负责：

- **executeAgent(String, String, ToolUseContext)**: 流式执行Agent任务
- **executeAgentSync(String, String, ToolUseContext)**: 同步执行Agent任务
- **clearSession(String)**: 清除会话历史
- **getSessionHistory(String)**: 获取会话历史

### Agent

可执行的Agent实例，包含：

- **execute(String, ToolUseContext)**: 执行对话任务
- **executeTool(String, I, ToolUseContext)**: 执行工具调用
- **getToolSchemas()**: 获取工具Schema列表
- **getAllowedTools()**: 获取允许的工具列表

### ConversationHistory

会话历史管理类：

- **addUserMessage(String)**: 添加用户消息
- **addAssistantMessage(String)**: 添加助手消息
- **addSystemMessage(String)**: 添加系统消息
- **getMessages()**: 获取所有消息
- **clear()**: 清除历史

### ChatMessage

单条消息记录：

- **role**: 消息角色(user/assistant/system)
- **content**: 消息内容
- **timestamp**: 时间戳
- **metadata**: 元数据

## 使用示例

### 基础使用

```java
@Autowired
private AgentExecutor agentExecutor;

public void example() {
    // 准备上下文
    ToolUseContext context = ToolUseContext.builder()
        .messageId("msg-123")
        .agentId("session-001")
        .build();
    
    // 流式执行
    agentExecutor.executeAgent("code-reviewer", "请审查这段代码", context)
        .subscribe(
            chunk -> System.out.print(chunk),
            error -> log.error("执行失败", error),
            () -> log.info("执行完成")
        );
    
    // 同步执行
    String result = agentExecutor.executeAgentSync("code-reviewer", "请审查这段代码", context);
    System.out.println(result);
}
```

### 会话管理

```java
// 创建会话并执行多轮对话
String sessionId = "my-session";

// 第一轮对话
ToolUseContext context = ToolUseContext.builder()
    .agentId(sessionId)
    .build();

agentExecutor.executeAgentSync("architect", "请设计一个电商系统", context);

// 第二轮对话（自动携带历史上下文）
agentExecutor.executeAgentSync("architect", "请详细说明用户模块", context);

// 查看历史
ConversationHistory history = agentExecutor.getSessionHistory(sessionId);
List<ChatMessage> messages = history.getMessages();

// 清除历史
agentExecutor.clearSession(sessionId);
```

### 工具权限控制

```java
// Agent配置中指定允许的工具
AgentConfig config = AgentConfig.builder()
    .agentType("read-only-agent")
    .tools(Arrays.asList("View", "Search", "Glob"))  // 只允许只读工具
    .systemPrompt("你只能查看和搜索代码，不能修改")
    .build();

// Agent执行时会自动过滤工具权限
Agent agent = agentExecutor.buildAgent(config, context);
List<Tool<?, ?>> allowedTools = agent.getAllowedTools();  // 只包含View, Search, Glob

// 尝试调用未授权的工具会抛出异常
agent.executeTool("Edit", input, context);  // 抛出AgentExecutionException
```

## Spring AI集成步骤

### 1. 添加Spring AI依赖

pom.xml中已添加：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### 2. 配置AI模型

在 `application.yml` 中配置：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7
```

### 3. 替换临时实现

当Spring AI依赖可用后，需要在 `Agent.execute()` 方法中：

```java
// 替换临时实现
public Flux<String> execute(String userInput, ToolUseContext context) {
    return Flux.create(sink -> {
        // 添加用户消息
        history.addUserMessage(userInput);
        
        // 准备消息列表
        List<Message> messages = convertToSpringAiMessages(history.getMessages());
        
        // 构建ChatClient请求
        StringBuilder fullResponse = new StringBuilder();
        
        chatClient.prompt()
            .messages(messages)
            .functions(getToolSchemas())  // 注册工具函数
            .stream()
            .content()
            .doOnNext(chunk -> {
                fullResponse.append(chunk);
                sink.next(chunk);
            })
            .doOnComplete(() -> {
                history.addAssistantMessage(fullResponse.toString());
                sink.complete();
            })
            .doOnError(sink::error)
            .subscribe();
    });
}

// 消息格式转换
private List<Message> convertToSpringAiMessages(List<ChatMessage> messages) {
    return messages.stream()
        .map(msg -> {
            switch (msg.getRole()) {
                case "user": return new UserMessage(msg.getContent());
                case "assistant": return new AssistantMessage(msg.getContent());
                case "system": return new SystemMessage(msg.getContent());
                default: throw new IllegalArgumentException("Unknown role: " + msg.getRole());
            }
        })
        .collect(Collectors.toList());
}
```

### 4. 实现Function Calling

```java
// 在Agent类中添加工具调用回调
private Function<ToolCall, String> createToolCallback(ToolUseContext context) {
    return toolCall -> {
        try {
            String toolName = toolCall.getName();
            Object args = toolCall.getArguments();
            
            // 执行工具
            String result = executeTool(toolName, args, context).block();
            return result;
        } catch (Exception e) {
            log.error("工具调用失败", e);
            return "Error: " + e.getMessage();
        }
    };
}
```

## 依赖关系

```
koder-agent
├── koder-core (核心模块)
├── koder-tools (工具模块)
├── Spring Boot Starter
├── Spring Boot WebFlux (响应式编程)
├── Spring AI OpenAI Starter (AI模型集成)
├── SnakeYAML (YAML解析)
├── Flexmark (Markdown解析)
└── Jackson (JSON处理)
```

## 注意事项

1. **Spring AI依赖**: 当前Spring AI依赖可能无法解析，需要确保Maven仓库配置正确
2. **临时实现**: `Agent.execute()` 方法使用了临时的模拟实现，需要在Spring AI可用后替换
3. **线程安全**: 会话历史使用ConcurrentHashMap管理，支持多线程访问
4. **工具权限**: Agent只能调用AgentConfig中配置的工具，未授权调用会抛出异常
5. **会话隔离**: 每个sessionId对应独立的会话历史，互不干扰

## 测试

```bash
cd Koder
mvn clean test -pl koder-agent
```

## 构建

```bash
mvn clean install -pl koder-agent
```

## TODO

- [ ] 集成真实的Spring AI ChatClient
- [ ] 实现Function Calling工具回调
- [ ] 添加流式响应的错误重试机制
- [ ] 支持多模型切换
- [ ] 添加会话持久化功能
- [ ] 实现对话历史的Token计数和截断
- [ ] 添加Agent执行的性能监控
- [ ] 支持并行执行多个Agent

## 参考文档

- [Spring AI官方文档](https://docs.spring.io/spring-ai/reference/)
- [ChatClient API文档](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Koder工具系统文档](../koder-tools/README.md)
- [Agent配置规范](../../AGENTS.md)
