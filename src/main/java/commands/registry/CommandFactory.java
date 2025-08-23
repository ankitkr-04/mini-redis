package commands.registry;

import blocking.BlockingManager;
import commands.impl.basic.EchoCommand;
import commands.impl.basic.PingCommand;
import commands.impl.basic.TypeCommand;
import commands.impl.lists.BlockingPopCommand;
import commands.impl.lists.LengthCommand;
import commands.impl.lists.PopCommand;
import commands.impl.lists.PushCommand;
import commands.impl.lists.RangeCommand;
import commands.impl.streams.AddStreamCommand;
import commands.impl.streams.RangeStreamCommand;
import commands.impl.streams.ReadStreamCommand;
import commands.impl.strings.GetCommand;
import commands.impl.strings.IncrCommand;
import commands.impl.strings.SetCommand;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import events.StorageEventPublisher;
import transaction.TransactionManager;

public final class CommandFactory {

    public static CommandRegistry createDefault(StorageEventPublisher eventPublisher,
            BlockingManager blockingManager, TransactionManager transactionManager) {
        CommandRegistry registry = new CommandRegistry();

        // Basic commands
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());

        // String commands
        registry.register(new GetCommand());
        registry.register(new SetCommand(eventPublisher));
        registry.register(new IncrCommand(eventPublisher));

        // List commands - simplified registration
        registry.register(new PushCommand(eventPublisher), "LPUSH", "RPUSH");
        registry.register(new PopCommand(), "LPOP", "RPOP");
        registry.register(new RangeCommand());
        registry.register(new LengthCommand());
        registry.register(new BlockingPopCommand(blockingManager));

        // Stream commands
        registry.register(new AddStreamCommand(eventPublisher));
        registry.register(new RangeStreamCommand());
        registry.register(new ReadStreamCommand(blockingManager));

        // Transaction commands
        registry.register(new MultiCommand(transactionManager, eventPublisher));
        registry.register(new ExecCommand(transactionManager, eventPublisher));
        registry.register(new DiscardCommand(transactionManager, eventPublisher));

        return registry;
    }
}
