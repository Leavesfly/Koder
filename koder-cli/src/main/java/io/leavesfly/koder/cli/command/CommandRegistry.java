package io.leavesfly.koder.cli.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令注册表
 * 负责命令的注册、查找和管理
 */
@Slf4j
@Component
public class CommandRegistry {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    /**
     * 注册命令
     */
    public void register(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        log.debug("注册命令: /{}", command.getName());
    }

    /**
     * 批量注册命令
     */
    public void registerAll(List<Command> commandList) {
        commandList.forEach(this::register);
        log.info("已注册 {} 个命令", commandList.size());
    }

    /**
     * 查找命令
     */
    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    /**
     * 获取所有命令
     */
    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * 检查是否为命令输入
     */
    public boolean isCommand(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.trim().startsWith("/");
    }

    /**
     * 解析命令输入
     */
    public CommandInput parse(String input) {
        if (!isCommand(input)) {
            return null;
        }

        String trimmed = input.trim().substring(1); // 移除 /
        String[] parts = trimmed.split("\\s+", 2);
        String commandName = parts[0];
        String argsString = parts.length > 1 ? parts[1] : "";

        return CommandInput.builder()
                .commandName(commandName)
                .args(parseArgs(argsString))
                .rawInput(input)
                .build();
    }

    /**
     * 解析参数列表
     */
    private List<String> parseArgs(String argsString) {
        if (argsString.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(argsString.split("\\s+"));
    }

    /**
     * 命令输入
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommandInput {
        private String commandName;
        private List<String> args;
        private String rawInput;
    }
}
