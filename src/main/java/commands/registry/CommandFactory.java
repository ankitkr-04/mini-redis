package commands.registry;

import commands.impl.basic.EchoCommand;
import commands.impl.basic.PingCommand;
import commands.impl.basic.TypeCommand;
import commands.impl.lists.PopCommand;
import commands.impl.lists.PushCommand;
import commands.impl.strings.GetCommand;
import commands.impl.strings.IncrCommand;
import commands.impl.strings.SetCommand;
import server.ServerContext;

public final class CommandFactory {

    public static CommandRegistry createRegistry(ServerContext context) {
        var registry = new CommandRegistry();

        // Basic commands
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());

        // String commands
        registry.register(new GetCommand());
        registry.register(new SetCommand());
        registry.register(new IncrCommand());

        // List commands
        registry.register(new PushCommand(), "LPUSH", "RPUSH");
        registry.register(new PopCommand(), "LPOP", "RPOP");

        // Additional commands would be registered here...

        return registry;
    }
}
