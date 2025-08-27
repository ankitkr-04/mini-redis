package commands.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import commands.impl.strings.DecrCommand;
import commands.impl.strings.GetCommand;
import commands.impl.strings.IncrCommand;
import commands.impl.strings.SetCommand;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import commands.impl.transaction.UnwatchCommand;
import commands.impl.transaction.WatchCommand;
import server.ServerContext;

/**
 * Factory for creating and registering all supported Redis command
 * implementations.
 * Registers commands into a CommandRegistry instance.
 */
public final class CommandFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandFactory.class);

    // Command names as constants
    private static final String LPUSH = "LPUSH";
    private static final String RPUSH = "RPUSH";
    private static final String LPOP = "LPOP";
    private static final String RPOP = "RPOP";
    private static final String SUBSCRIBE = "SUBSCRIBE";
    private static final String PSUBSCRIBE = "PSUBSCRIBE";
    private static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    private static final String PUNSUBSCRIBE = "PUNSUBSCRIBE";

    private CommandFactory() {
        // Prevent instantiation
    }

    /**
     * Creates and returns a CommandRegistry with all supported commands registered.
     *
     * @param serverContext the server context required for certain commands
     * @return a populated CommandRegistry instance
     */
    public static CommandRegistry createRegistry(ServerContext serverContext) {
        CommandRegistry commandRegistry = new CommandRegistry();

        registerBasicCommands(commandRegistry);
        registerKeyCommands(commandRegistry);
        registerConfigCommands(commandRegistry);
        registerStringCommands(commandRegistry);
        registerListCommands(commandRegistry, serverContext);
        registerStreamCommands(commandRegistry);
        registerTransactionCommands(commandRegistry);
        registerReplicationCommands(commandRegistry);
        registerPubSubCommands(commandRegistry);
        registerSortedSetCommands(commandRegistry);

        LOGGER.debug("All commands registered successfully.");
        return commandRegistry;
    }

    private static void registerBasicCommands(CommandRegistry registry) {
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());
    }

    private static void registerKeyCommands(CommandRegistry registry) {
        registry.register(new KeysComamnd());
    }

    private static void registerConfigCommands(CommandRegistry registry) {
        registry.register(new InfoCommand());
        registry.register(new ConfigCommand());
        registry.register(new MetricsCommand());
    }

    private static void registerStringCommands(CommandRegistry registry) {
        registry.register(new GetCommand());
        registry.register(new SetCommand());
        registry.register(new IncrCommand());
        registry.register(new DecrCommand());
    }

    private static void registerListCommands(CommandRegistry registry, ServerContext context) {
        registry.register(new PushCommand(), LPUSH, RPUSH);
        registry.register(new PopCommand(), LPOP, RPOP);
        registry.register(new LengthCommand());
        registry.register(new RangeCommand());
        registry.register(new BlockingPopCommand(context.getBlockingManager()));
    }

    private static void registerStreamCommands(CommandRegistry registry) {
        registry.register(new AddStreamCommand());
        registry.register(new RangeStreamCommand());
        registry.register(new ReadStreamCommand());
    }

    private static void registerTransactionCommands(CommandRegistry registry) {
        registry.register(new MultiCommand());
        registry.register(new ExecCommand());
        registry.register(new DiscardCommand());
        registry.register(new WatchCommand());
        registry.register(new UnwatchCommand());
    }

    private static void registerReplicationCommands(CommandRegistry registry) {
        registry.register(new PsyncCommand());
        registry.register(new ReplconfCommand());
        registry.register(new WaitCommand());
    }

    private static void registerPubSubCommands(CommandRegistry registry) {
        registry.register(new SubscribeCommand(), SUBSCRIBE, PSUBSCRIBE);
        registry.register(new UnsubscribeCommand(), UNSUBSCRIBE, PUNSUBSCRIBE);
        registry.register(new PublishCommand());
    }

    private static void registerSortedSetCommands(CommandRegistry registry) {
        registry.register(new ZAddCommand());
        registry.register(new ZCardCommand());
        registry.register(new ZRangeCommand());
        registry.register(new ZRankCommand());
        registry.register(new ZRemCommand());
        registry.register(new ZScoreCommand());
    }
}
