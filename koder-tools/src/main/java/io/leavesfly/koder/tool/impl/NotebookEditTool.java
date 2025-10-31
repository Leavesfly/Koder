package io.leavesfly.koder.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.leavesfly.koder.tool.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Jupyter Notebook 编辑工具
 */
@Slf4j
@Component
public class NotebookEditTool extends AbstractTool<NotebookEditTool.Input, NotebookEditTool.Output> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "NotebookEditCell";
    }

    @Override
    public String getDescription() {
        return "编辑 Jupyter Notebook (.ipynb) 文件中的单元格";
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return """
                Jupyter Notebook 编辑工具
                
                用法：
                - notebook_path: Notebook 文件的绝对路径（.ipynb 文件）
                - cell_number: 要编辑的单元格索引（从 0 开始）
                - new_source: 单元格的新内容
                - cell_type: 单元格类型（code 或 markdown），可选
                - edit_mode: 编辑模式（replace/insert/delete），默认 replace
                
                编辑模式：
                - replace: 替换指定单元格的内容
                - insert: 在指定位置插入新单元格
                - delete: 删除指定单元格
                
                注意事项：
                - 仅适用于 .ipynb 文件
                - 编辑 code 单元格会清除执行计数和输出
                - insert 模式需要指定 cell_type
                - 对于普通文本文件，请使用 FileEdit 或 MultiEdit 工具
                """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "notebook_path", Map.of(
                                "type", "string",
                                "description", "Notebook 文件的绝对路径（.ipynb）"
                        ),
                        "cell_number", Map.of(
                                "type", "integer",
                                "description", "单元格索引（从 0 开始）"
                        ),
                        "new_source", Map.of(
                                "type", "string",
                                "description", "单元格的新内容"
                        ),
                        "cell_type", Map.of(
                                "type", "string",
                                "enum", List.of("code", "markdown"),
                                "description", "单元格类型（可选，insert 模式必需）"
                        ),
                        "edit_mode", Map.of(
                                "type", "string",
                                "enum", List.of("replace", "insert", "delete"),
                                "description", "编辑模式（默认 replace）"
                        )
                ),
                "required", List.of("notebook_path", "cell_number", "new_source")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        return false;
    }

    @Override
    public boolean needsPermissions(Input input) {
        return true;
    }

    @Override
    public String renderToolUseMessage(Input input, boolean verbose) {
        String mode = input.editMode != null ? input.editMode : "replace";
        return String.format("编辑 Notebook: %s, 单元格 %d, 模式: %s",
                input.notebookPath, input.cellNumber, mode);
    }

    @Override
    public String renderToolResultMessage(Output output) {
        if (output.error != null) {
            return "❌ " + output.error;
        }
        return String.format("✅ 成功%s单元格 %d (%s)",
                output.editMode, output.cellNumber, output.cellType);
    }

    @Override
    public Flux<ToolResponse<Output>> call(Input input, ToolUseContext context) {
        return Flux.create(sink -> {
            try {
                Path notebookPath = Paths.get(input.notebookPath);
                
                if (!Files.exists(notebookPath)) {
                    Output output = Output.builder()
                            .error("Notebook 文件不存在: " + input.notebookPath)
                            .build();
                    sink.next(ToolResponse.result(output));
                    sink.complete();
                    return;
                }
                
                if (!input.notebookPath.endsWith(".ipynb")) {
                    Output output = Output.builder()
                            .error("文件必须是 .ipynb 格式，其他文件请使用 FileEdit 工具")
                            .build();
                    sink.next(ToolResponse.result(output));
                    sink.complete();
                    return;
                }
                
                // 读取并解析 notebook
                String content = Files.readString(notebookPath);
                JsonNode notebook = objectMapper.readTree(content);
                
                if (!notebook.has("cells") || !notebook.get("cells").isArray()) {
                    Output output = Output.builder()
                            .error("Notebook 格式无效")
                            .build();
                    sink.next(ToolResponse.result(output));
                    sink.complete();
                    return;
                }
                
                ArrayNode cells = (ArrayNode) notebook.get("cells");
                String editMode = input.editMode != null ? input.editMode : "replace";
                String language = notebook.path("metadata").path("language_info")
                        .path("name").asText("python");
                
                // 执行编辑操作
                switch (editMode) {
                    case "delete":
                        if (input.cellNumber < 0 || input.cellNumber >= cells.size()) {
                            Output output = Output.builder()
                                    .error(String.format("单元格索引超出范围。Notebook 有 %d 个单元格",
                                            cells.size()))
                                    .build();
                            sink.next(ToolResponse.result(output));
                            sink.complete();
                            return;
                        }
                        cells.remove(input.cellNumber);
                        break;
                        
                    case "insert":
                        if (input.cellType == null) {
                            Output output = Output.builder()
                                    .error("insert 模式需要指定 cell_type")
                                    .build();
                            sink.next(ToolResponse.result(output));
                            sink.complete();
                            return;
                        }
                        if (input.cellNumber < 0 || input.cellNumber > cells.size()) {
                            Output output = Output.builder()
                                    .error(String.format("单元格索引超出范围。insert 模式最大值为 %d",
                                            cells.size()))
                                    .build();
                            sink.next(ToolResponse.result(output));
                            sink.complete();
                            return;
                        }
                        ObjectNode newCell = createCell(input.cellType, input.newSource);
                        cells.insert(input.cellNumber, newCell);
                        break;
                        
                    case "replace":
                    default:
                        if (input.cellNumber < 0 || input.cellNumber >= cells.size()) {
                            Output output = Output.builder()
                                    .error(String.format("单元格索引超出范围。Notebook 有 %d 个单元格",
                                            cells.size()))
                                    .build();
                            sink.next(ToolResponse.result(output));
                            sink.complete();
                            return;
                        }
                        ObjectNode targetCell = (ObjectNode) cells.get(input.cellNumber);
                        targetCell.put("source", input.newSource);
                        targetCell.remove("execution_count");
                        targetCell.remove("outputs");
                        if (input.cellType != null) {
                            targetCell.put("cell_type", input.cellType);
                        }
                        if ("code".equals(targetCell.get("cell_type").asText())) {
                            targetCell.set("outputs", objectMapper.createArrayNode());
                        }
                        break;
                }
                
                // 写回文件
                String updatedContent = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(notebook);
                Files.writeString(notebookPath, updatedContent);
                
                log.info("成功编辑 Notebook: {}, 单元格 {}, 模式: {}",
                        input.notebookPath, input.cellNumber, editMode);
                
                Output output = Output.builder()
                        .cellNumber(input.cellNumber)
                        .newSource(input.newSource)
                        .cellType(input.cellType != null ? input.cellType : "code")
                        .language(language)
                        .editMode(editMode)
                        .build();
                
                sink.next(ToolResponse.result(output));
                sink.complete();
                
            } catch (IOException e) {
                log.error("Notebook 编辑失败", e);
                Output output = Output.builder()
                        .error("文件操作失败: " + e.getMessage())
                        .build();
                sink.next(ToolResponse.result(output));
                sink.complete();
            } catch (Exception e) {
                log.error("Notebook 编辑失败", e);
                sink.error(new RuntimeException("Notebook 编辑失败: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 创建新单元格
     */
    private ObjectNode createCell(String cellType, String source) {
        ObjectNode cell = objectMapper.createObjectNode();
        cell.put("cell_type", cellType);
        cell.put("source", source);
        cell.set("metadata", objectMapper.createObjectNode());
        
        if ("code".equals(cellType)) {
            cell.set("outputs", objectMapper.createArrayNode());
        }
        
        return cell;
    }
    
    /**
     * 输入参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String notebookPath;
        private Integer cellNumber;
        private String newSource;
        private String cellType;
        private String editMode;
    }
    
    /**
     * 输出结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private Integer cellNumber;
        private String newSource;
        private String cellType;
        private String language;
        private String editMode;
        private String error;
    }
}
