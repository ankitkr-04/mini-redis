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
import commands.strings.GetCommand;
import commands.strings.SetCommand;
import storage.interfaces.StorageEngine;

public final class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public void register(Command command) {
        commands.put(command.name().toUpperCase(), command);
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

        // List commands
        var pushCommand = new PushCommand(blockingManager);
        registry.register(new Command() {
            @Override
            public String name() {
                return "LPUSH";
            }

            @Override
            public CommandResult execute(CommandArgs args, StorageEngine storage) {
                return pushCommand.execute(args, storage);
            }

            @Override
            public boolean validate(CommandArgs args) {
                return pushCommand.validate(args);
            }
        });

        registry.register(new Command() {
            @Override
            public String name() {
                return "RPUSH";
            }

            @Override
            public CommandResult execute(CommandArgs args, StorageEngine storage) {
                return pushCommand.execute(args, storage);
            }

            @Override
            public boolean validate(CommandArgs args) {
                return pushCommand.validate(args);
            }
        });

        var popCommand = new PopCommand();
        registry.register(new Command() {
            @Override
            public String name() {
                return "LPOP";
            }

            @Override
            public CommandResult execute(CommandArgs args, StorageEngine storage) {
                return popCommand.execute(args, storage);
            }

            @Override
            public boolean validate(CommandArgs args) {
                return popCommand.validate(args);
            }
        });

        registry.register(new Command() {
            @Override
            public String name() {
                return "RPOP";
            }

            @Override
            public CommandResult execute(CommandArgs args, StorageEngine storage) {
                return popCommand.execute(args, storage);
            }

            @Override
            public boolean validate(CommandArgs args) {
                return popCommand.validate(args);
            }
        });

        registry.register(new RangeCommand());
        registry.register(new LengthCommand());
        registry.register(new BlockingPopCommand(blockingManager));


        // Stream commands
        registry.register(new AddStreamCommand());
        return registry;
    }

}
