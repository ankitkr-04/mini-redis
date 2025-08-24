package commands.registry;

import commands.impl.basic.EchoCommand;
import commands.impl.basic.InfoCommand;
import commands.impl.basic.PingCommand;
import commands.impl.basic.TypeCommand;
import commands.impl.lists.BlockingPopCommand;
import commands.impl.lists.LengthCommand;
import commands.impl.lists.PopCommand;
import commands.impl.lists.PushCommand;
import commands.impl.lists.RangeCommand;
import commands.impl.replication.PsyncCommand;
import commands.impl.replication.ReplconfCommand;
import commands.impl.streams.AddStreamCommand;
import commands.impl.streams.RangeStreamCommand;
import commands.impl.streams.ReadStreamCommand;
import commands.impl.strings.GetCommand;
import commands.impl.strings.IncrCommand;
import commands.impl.strings.SetCommand;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import core.ServerContext;

public final class CommandFactory {

    public static CommandRegistry createDefault(ServerContext context) {

        var registry = new CommandRegistry();

        // Basic commands
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());
        registry.register(new InfoCommand(context.getServerInfo()));

        // Replication commands

        registry.register(new ReplconfCommand(context));
        registry.register(new PsyncCommand(context));

        // String commands
        registry.register(new GetCommand());
        registry.register(new SetCommand(context));
        registry.register(new IncrCommand(context));

        // List commands
        registry.register(new PushCommand(context), "LPUSH", "RPUSH");
        registry.register(new PopCommand(), "LPOP", "RPOP");
        registry.register(new RangeCommand());
        registry.register(new LengthCommand());
        registry.register(new BlockingPopCommand(context.getBlockingManager()));

        // Stream commands
        registry.register(new AddStreamCommand(context));
        registry.register(new RangeStreamCommand());
        registry.register(new ReadStreamCommand(context.getBlockingManager()));

        // Transaction commands
        registry.register(new MultiCommand(context.getTransactionManager(), context));
        registry.register(new ExecCommand(context.getTransactionManager(), context));
        registry.register(new DiscardCommand(context.getTransactionManager(), context));

        return registry;
    }
}
