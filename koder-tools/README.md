# Koder Tools - 工具系统模块

## 概述

koder-tools 是 Koder 项目的工具系统模块，提供了一套完整的可扩展工具框架，支持文件操作、命令执行、搜索、网络访问等功能。

## 架构设计

### 核心组件

1. **Tool 接口** - 所有工具的基础契约
2. **AbstractTool** - 工具抽象基类，简化工具开发
3. **ToolExecutor** - 工具执行引擎，负责工具的注册、管理和执行
4. **ToolUseContext** - 工具执行上下文，包含执行所需的全部信息
5. **ToolResponse** - 流式响应，支持进度更新和最终结果

### 工具分类

#### 文件操作工具
- **FileReadTool** - 读取文件内容（支持分页）
- **FileWriteTool** - 创建或覆盖文件
- **FileEditTool** - 基于搜索-替换的文件编辑

#### 搜索工具
- **GlobTool** - 使用Glob模式搜索文件
- **GrepTool** - 使用正则表达式在文件中搜索内容
- **LSTool** - 列出目录内容

#### 系统工具
- **BashTool** - 执行Shell命令

#### AI辅助工具
- **ThinkTool** - 记录AI思考过程（支持思维链推理）
- **TaskTool** - 任务管理（添加、更新、查询任务）
- **AskExpertTool** - 向专家模型咨询

#### 网络工具
- **URLFetcherTool** - 从URL获取内容
- **WebSearchTool** - 网络搜索（模拟实现）

#### 记忆工具
- **MemoryReadTool** - 从记忆系统检索信息
- **MemoryWriteTool** - 向记忆系统存储信息

## 使用方法

### 1. 创建自定义工具

```java
@Component
public class MyCustomTool extends AbstractTool<MyCustomTool.Input, MyCustomTool.Output> {
    
    @Override
    public String getName() {
        return "MyTool";
    }
    
    @Override
    public String getDescription() {
        return "我的自定义工具";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return schema()
            .addStringProperty("param", "参数描述")
            .required("param")
            .build();
    }
    
    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            // 工具逻辑
            Output output = new Output();
            sink.next(ToolResponse.result(output));
            sink.complete();
        });
    }
    
    @Data
    public static class Input {
        private String param;
    }
    
    @Data
    public static class Output {
        private String result;
    }
}
```

### 2. 执行工具

```java
@Autowired
private ToolExecutor toolExecutor;

public void executeExample() {
    // 准备输入
    FileReadTool.Input input = FileReadTool.Input.builder()
        .filePath("/path/to/file.txt")
        .build();
    
    // 准备上下文
    ToolUseContext context = ToolUseContext.builder()
        .messageId("msg-123")
        .abortController(new AbortController())
        .readFileTimestamps(new HashMap<>())
        .build();
    
    // 执行工具
    toolExecutor.execute("View", input, context)
        .subscribe(
            response -> {
                if (response.getType() == ToolResponse.ResponseType.RESULT) {
                    System.out.println("结果: " + response.getData());
                }
            },
            error -> System.err.println("错误: " + error.getMessage())
        );
}
```

### 3. 获取工具Schema

```java
// 获取单个工具的Schema
Map<String, Object> schema = toolExecutor.getToolSchema("View");

// 获取所有工具的Schema列表
List<Map<String, Object>> allSchemas = toolExecutor.getAllToolSchemas();
```

## 工具特性

### 1. 响应式流式执行
所有工具使用 Project Reactor 的 `Flux` 进行流式处理，支持：
- 进度更新
- 取消操作（通过AbortController）
- 背压控制

### 2. 验证机制
- 输入验证（validateInput）
- 权限检查（needsPermissions）
- 安全限制

### 3. 并发控制
- `isConcurrencySafe()` 标识工具是否支持并发执行
- 只读工具通常支持并发，修改性工具不支持

### 4. 错误处理
工具执行器提供统一的异常处理：
- `ToolNotFoundException` - 工具不存在
- `ToolValidationException` - 输入验证失败
- `ToolExecutionException` - 执行异常
- `ToolAbortedException` - 操作被中断

## 配置

在 `application.yml` 中配置：

```yaml
koder:
  tools:
    list-enabled: true  # 是否启用工具列表Bean
  think:
    enabled: true       # 是否启用ThinkTool
```

## 扩展点

1. **自定义工具** - 继承 `AbstractTool` 并使用 `@Component` 注册
2. **工具过滤** - 在 `ToolConfiguration` 中添加条件注册逻辑
3. **持久化** - 实现记忆系统的持久化存储（当前为内存实现）
4. **专家模型集成** - 为 `AskExpertTool` 集成实际的模型调用

## 注意事项

1. **安全性** - BashTool 禁止执行危险命令（rm, shutdown等）
2. **性能** - 大文件读取自动分页，搜索结果自动截断
3. **并发** - 文件修改操作需要串行执行
4. **资源管理** - 长时间运行的命令支持超时控制

## 依赖

- Spring Boot 3.2.5
- Project Reactor
- Apache Commons Exec
- Apache Commons IO
- Lombok
- SLF4J

## 测试

```bash
cd Koder
mvn test -pl koder-tools
```

## 构建

```bash
mvn clean install -pl koder-tools
```
