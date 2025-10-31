package io.leavesfly.koder.tool;

import java.util.Map;

/**
 * 工具抽象基类
 * 提供工具接口的默认实现，简化具体工具的开发
 *
 * @param <I> 输入参数类型
 * @param <O> 输出结果类型
 */
public abstract class AbstractTool<I, O> implements Tool<I, O> {

    @Override
    public String getUserFacingName() {
        return getName();
    }

    @Override
    public boolean isEnabled() {
        return true;
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
    public boolean needsPermissions(I input) {
        return !isReadOnly();
    }

    @Override
    public ValidationResult validateInput(I input, ToolUseContext context) {
        return ValidationResult.success();
    }

    @Override
    public String renderToolResultMessage(O output) {
        return String.valueOf(output);
    }

    @Override
    public Object renderResultForAssistant(O output) {
        return output;
    }

    @Override
    public String getPrompt(boolean safeMode) {
        return getDescription();
    }

    /**
     * 构建JSON Schema的辅助方法
     *
     * @return Schema构建器
     */
    protected SchemaBuilder schema() {
        return new SchemaBuilder();
    }

    /**
     * JSON Schema构建器
     */
    protected static class SchemaBuilder {
        private final Map<String, Object> schema = new java.util.LinkedHashMap<>();
        private final Map<String, Object> properties = new java.util.LinkedHashMap<>();
        private final java.util.List<String> required = new java.util.ArrayList<>();

        public SchemaBuilder() {
            schema.put("type", "object");
            schema.put("properties", properties);
        }

        public SchemaBuilder addProperty(String name, String type, String description) {
            Map<String, Object> prop = new java.util.HashMap<>();
            prop.put("type", type);
            prop.put("description", description);
            properties.put(name, prop);
            return this;
        }

        public SchemaBuilder addStringProperty(String name, String description) {
            return addProperty(name, "string", description);
        }

        public SchemaBuilder addNumberProperty(String name, String description) {
            return addProperty(name, "number", description);
        }

        public SchemaBuilder addBooleanProperty(String name, String description) {
            return addProperty(name, "boolean", description);
        }

        public SchemaBuilder required(String... names) {
            required.addAll(java.util.Arrays.asList(names));
            return this;
        }

        public Map<String, Object> build() {
            if (!required.isEmpty()) {
                schema.put("required", required);
            }
            return schema;
        }
    }
}
