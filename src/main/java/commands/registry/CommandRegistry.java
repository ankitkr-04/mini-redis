package commands.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import commands.Command;

public final class CommandRegistry {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(Command command) {
        commands.put(command.name().toUpperCase(), command);
    }

    public void register(Command command, String... additionalNames) {
        register(command);
        for (String name : additionalNames) {
            commands.put(name.toUpperCase(), command);
        }
    }

    public Command getCommand(String name) {
        return commands.get(name.toUpperCase());
    }


}
