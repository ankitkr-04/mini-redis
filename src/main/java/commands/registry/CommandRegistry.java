package commands.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import storage.StorageService;

public final class CommandRegistry {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(Command command) {
        commands.put(command.name().toUpperCase(), command);
    }

    public void register(String name, Command baseCommand) {
        commands.put(name.toUpperCase(), new CommandAdapter(name, baseCommand));
    }

    public Command getCommand(String name) {
        return commands.get(name.toUpperCase());
    }

    // Adapter for commands with different names
    private static class CommandAdapter implements Command {
        private final String name;
        private final Command delegate;

        public CommandAdapter(String name, Command delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public CommandResult execute(CommandArgs args, StorageService storage) {
            return delegate.execute(args, storage);
        }

        @Override
        public boolean validate(CommandArgs args) {
            return delegate.validate(args);
        }

        @Override
        public boolean requiresClient() {
            return delegate.requiresClient();
        }
    }
}
