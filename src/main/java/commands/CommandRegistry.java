package commands;

import java.util.HashMap;
import java.util.Map;
import blocking.BlockingManager;
import commands.basic.EchoCommand;
import commands.basic.PingCommand;
import commands.basic.TypeCommand;
import commands.lists.BlockingPopCommand;
import commands.lists.LengthCommand;
import commands.lists.PopCommand;
import commands.lists.PushCommand;
import commands.lists.RangeCommand;
import commands.streams.AddStreamCommand;
import commands.streams.RangeStreamCommand;
import commands.streams.ReadStreamCommand;
import commands.strings.GetCommand;
import commands.strings.SetCommand;
import storage.interfaces.StorageEngine;

public final class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public void register(Command command) {
        commands.put(command.name().toUpperCase(), command);
    }

    public void register(String name, Command baseCommand) {
        commands.put(name.toUpperCase(), new CommandAdapter(name, baseCommand));
    }

    public Command getCommand(String name) {
        return commands.get(name.toUpperCase());
    }

    public static CommandRegistry createDefault(BlockingManager blockingManager) {
        CommandRegistry registry = new CommandRegistry();

        // Basic commands
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());

        // String commands
        registry.register(new GetCommand());
        registry.register(new SetCommand());

        // List commands with shared implementations
        var pushCommand = new PushCommand(blockingManager);
        registry.register("LPUSH", pushCommand);
        registry.register("RPUSH", pushCommand);

        var popCommand = new PopCommand();
        registry.register("LPOP", popCommand);
        registry.register("RPOP", popCommand);

        registry.register(new RangeCommand());
        registry.register(new LengthCommand());
        registry.register(new BlockingPopCommand(blockingManager));

        // Stream commands
        registry.register(new AddStreamCommand());
        registry.register(new RangeStreamCommand());
        registry.register(new ReadStreamCommand());
        return registry;
    }

    // Adapter to wrap commands with different names
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
        public CommandResult execute(CommandArgs args, StorageEngine storage) {
            return delegate.execute(args, storage);
        }

        @Override
        public boolean validate(CommandArgs args) {
            return delegate.validate(args);
        }
    }
}
