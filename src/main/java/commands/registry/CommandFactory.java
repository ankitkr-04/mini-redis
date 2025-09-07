package commands.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.impl.basic.EchoCommand;
import commands.impl.basic.PingCommand;
import commands.impl.basic.TypeCommand;
import commands.impl.config.ConfigCommand;
import commands.impl.config.FlushAllCommand;
import commands.impl.config.InfoCommand;
import commands.impl.config.MetricsCommand;
import commands.impl.keys.KeysComamnd;
import commands.impl.lists.*;
import commands.impl.pubsub.PublishCommand;
import commands.impl.pubsub.SubscribeCommand;
import commands.impl.pubsub.UnsubscribeCommand;
import commands.impl.replication.PsyncCommand;
import commands.impl.replication.ReplconfCommand;
import commands.impl.replication.WaitCommand;
import commands.impl.sortedsets.*;
import commands.impl.streams.AddStreamCommand;
import commands.impl.streams.RangeStreamCommand;
import commands.impl.streams.ReadStreamCommand;
import commands.impl.strings.DecrCommand;
import commands.impl.strings.GetCommand;
import commands.impl.strings.IncrCommand;
import commands.impl.strings.SetCommand;
import commands.impl.transaction.*;
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
    public static CommandRegistry createRegistry(final ServerContext serverContext) {
        final CommandRegistry commandRegistry = new CommandRegistry();

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

    private static void registerBasicCommands(final CommandRegistry registry) {
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        registry.register(new TypeCommand());
    }

    private static void registerKeyCommands(final CommandRegistry registry) {
        registry.register(new KeysComamnd());
    }

    private static void registerConfigCommands(final CommandRegistry registry) {
        registry.register(new InfoCommand());
        registry.register(new ConfigCommand());
        registry.register(new MetricsCommand());
        registry.register(new FlushAllCommand());
    }

    private static void registerStringCommands(final CommandRegistry registry) {
        registry.register(new GetCommand());
        registry.register(new SetCommand());
        registry.register(new IncrCommand());
        registry.register(new DecrCommand());
    }

    private static void registerListCommands(final CommandRegistry registry, final ServerContext context) {
        registry.register(new PushCommand(), LPUSH, RPUSH);
        registry.register(new PopCommand(), LPOP, RPOP);
        registry.register(new LengthCommand());
        registry.register(new RangeCommand());
        registry.register(new BlockingPopCommand(context.getBlockingManager()));
    }

    private static void registerStreamCommands(final CommandRegistry registry) {
        registry.register(new AddStreamCommand());
        registry.register(new RangeStreamCommand());
        registry.register(new ReadStreamCommand());
    }

    private static void registerTransactionCommands(final CommandRegistry registry) {
        registry.register(new MultiCommand());
        registry.register(new ExecCommand());
        registry.register(new DiscardCommand());
        registry.register(new WatchCommand());
        registry.register(new UnwatchCommand());
    }

    private static void registerReplicationCommands(final CommandRegistry registry) {
        registry.register(new PsyncCommand());
        registry.register(new ReplconfCommand());
        registry.register(new WaitCommand());
    }

    private static void registerPubSubCommands(final CommandRegistry registry) {
        registry.register(new SubscribeCommand(), SUBSCRIBE, PSUBSCRIBE);
        registry.register(new UnsubscribeCommand(), UNSUBSCRIBE, PUNSUBSCRIBE);
        registry.register(new PublishCommand());
    }

    private static void registerSortedSetCommands(final CommandRegistry registry) {
        registry.register(new ZAddCommand());
        registry.register(new ZCardCommand());
        registry.register(new ZRangeCommand());
        registry.register(new ZRankCommand());
        registry.register(new ZRemCommand());
        registry.register(new ZScoreCommand());
    }
}
