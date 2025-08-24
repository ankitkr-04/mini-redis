package commands.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import commands.core.Command;

public final class CommandRegistry {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(Command command) {
        commands.put(command.getName().toUpperCase(), command);
    }

    public void register(Command command, String... aliases) {
        register(command);
        for (String alias : aliases) {
            commands.put(alias.toUpperCase(), command);
        }
    }

    public Command getCommand(String name) {
        return commands.get(name.toUpperCase());
    }

    public boolean hasCommand(String name) {
        return commands.containsKey(name.toUpperCase());
    }

    public int size() {
        return commands.size();
    }
}
