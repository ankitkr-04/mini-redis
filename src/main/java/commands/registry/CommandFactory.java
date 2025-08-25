package commands.registry;

import commands.impl.basic.EchoCommand;
import commands.impl.basic.PingCommand;
import commands.impl.basic.TypeCommand;
import commands.impl.config.ConfigCommand;
import commands.impl.config.InfoCommand;
import commands.impl.config.MetricsCommand;
import commands.impl.keys.KeysComamnd;
import commands.impl.lists.BlockingPopCommand;
import commands.impl.lists.LengthCommand;
import commands.impl.lists.PopCommand;
import commands.impl.lists.PushCommand;
import commands.impl.lists.RangeCommand;
import commands.impl.pubsub.PublishCommand;
import commands.impl.pubsub.SubscribeCommand;
import commands.impl.pubsub.UnsubscribeCommand;
import commands.impl.replication.PsyncCommand;
import commands.impl.replication.ReplconfCommand;
import commands.impl.replication.WaitCommand;
import commands.impl.sortedsets.ZAddCommand;
import commands.impl.sortedsets.ZCardCommand;
import commands.impl.sortedsets.ZRangeCommand;
import commands.impl.sortedsets.ZRankCommand;
import commands.impl.sortedsets.ZRemCommand;
import commands.impl.sortedsets.ZScoreCommand;
import commands.impl.streams.AddStreamCommand;
import commands.impl.streams.RangeStreamCommand;
import commands.impl.streams.ReadStreamCommand;
import commands.impl.strings.GetCommand;
import commands.impl.strings.IncrCommand;
import commands.impl.strings.SetCommand;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import commands.impl.transaction.UnwatchCommand;
import commands.impl.transaction.WatchCommand;
import server.ServerContext;

public final class CommandFactory {

    public static CommandRegistry createRegistry(ServerContext context) {
        var registry = new CommandRegistry();

        // Basic commands
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());

        // Keys commands
        registry.register(new KeysComamnd());

        // Configuration commands
        registry.register(new InfoCommand());
        registry.register(new ConfigCommand());
        registry.register(new MetricsCommand());

        // String commands
        registry.register(new GetCommand());
        registry.register(new SetCommand());
        registry.register(new IncrCommand());

        // List commands
        registry.register(new PushCommand(), "LPUSH", "RPUSH");
        registry.register(new PopCommand(), "LPOP", "RPOP");
        registry.register(new LengthCommand());
        registry.register(new RangeCommand());
        registry.register(new BlockingPopCommand(context.getBlockingManager()));

        // Streams commands
        registry.register(new AddStreamCommand());
        registry.register(new RangeStreamCommand());
        registry.register(new ReadStreamCommand());

        // Transaction commands
        registry.register(new MultiCommand());
        registry.register(new ExecCommand());
        registry.register(new DiscardCommand());
        registry.register(new WatchCommand());
        registry.register(new UnwatchCommand());

        // Replication commands
        registry.register(new PsyncCommand());
        registry.register(new ReplconfCommand());
        registry.register(new WaitCommand());

        // PubSubCommand
        registry.register(new SubscribeCommand(), "SUBSCRIBE", "PSUBSCRIBE");
        registry.register(new UnsubscribeCommand(), "UNSUBSCRIBE", "PUNSUBSCRIBE");
        registry.register(new PublishCommand());

        // SortedSet commands
        registry.register(new ZAddCommand());
        registry.register(new ZCardCommand());
        registry.register(new ZRangeCommand());
        registry.register(new ZRankCommand());
        registry.register(new ZRemCommand());
        registry.register(new ZScoreCommand());
        return registry;
    }
}
