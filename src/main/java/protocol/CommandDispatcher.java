package protocol;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.context.CommandContext;
import commands.core.Command;
import commands.impl.basic.PingCommand;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import commands.registry.CommandRegistry;
import commands.result.CommandResult;
import errors.ErrorCode;
import pubsub.PubSubManager;
import server.ServerContext;
import storage.StorageService;
import transaction.TransactionManager;
import transaction.TransactionState;

/**
 * Central command dispatcher for processing Redis protocol commands.
 * 
 * <p>
 * This class is responsible for receiving raw command arguments, validating
 * them,
 * looking up the appropriate command handler, and executing the command with
 * proper
 * context. It handles transaction state, pub/sub mode restrictions, and command
 * propagation for replication.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class CommandDispatcher {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDispatcher.class);

    /** Minimum number of arguments required for a valid command */
    private static final int MINIMUM_COMMAND_ARGS = 1;
    private final CommandRegistry registry;
    private final StorageService storage;
    private final TransactionManager transactionManager;
    private final PubSubManager pubSubManager;
    private final ServerContext context;

    /**
     * Constructs a new CommandDispatcher with the required dependencies.
     * 
     * @param registry           the command registry for looking up commands
     * @param storage            the storage service for data operations
     * @param transactionManager the transaction manager for handling MULTI/EXEC
     * @param pubSubManager      the pub/sub manager for subscription handling
     * @param context            the server context containing shared resources
     */
    public CommandDispatcher(final CommandRegistry registry,
            final StorageService storage,
            final TransactionManager transactionManager,
            final PubSubManager pubSubManager,
            final ServerContext context) {
        this.registry = registry;
        this.storage = storage;
        this.pubSubManager = pubSubManager;
        this.transactionManager = transactionManager;
        this.context = context;
    }

    /**
     * Dispatches a command with the given arguments.
     * 
     * @param rawArgs       the raw command arguments
     * @param clientChannel the client socket channel
     * @return the response buffer, or null if no response needed
     */
    public ByteBuffer dispatch(final String[] rawArgs, final SocketChannel clientChannel) {
        return dispatch(rawArgs, clientChannel, false);
    }

    /**
     * Dispatches a command with the given arguments and propagation flag.
     * 
     * @param rawArgs             the raw command arguments
     * @param clientChannel       the client socket channel
     * @param isPropagatedCommand whether this is a propagated command from master
     * @return the response buffer, or null if no response needed
     */
    public ByteBuffer dispatch(final String[] rawArgs,
            final SocketChannel clientChannel,
            final boolean isPropagatedCommand) {
        if (!isValidCommandInput(rawArgs)) {
            return ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.getMessage());
        }

        final String commandName = rawArgs[0].toUpperCase();
        final Command command = registry.getCommand(commandName);

        if (command == null) {
            return handleUnknownCommand(commandName, isPropagatedCommand);
        }

        final CommandContext cmdContext = new CommandContext(commandName, rawArgs, clientChannel, storage, context);

        if (!command.validate(cmdContext)) {
            return handleValidationFailure(isPropagatedCommand);
        }

        if (!canExecuteInCurrentMode(clientChannel, command)) {
            return ResponseBuilder.error(ErrorCode.NOT_ALLOWED_IN_PUBSUB_MODE.format(commandName.toLowerCase()));
        }

        return executeCommand(command, cmdContext, clientChannel, isPropagatedCommand, rawArgs);
    }

    /**
     * Validates that command input is not null and has at least one argument.
     * 
     * @param rawArgs the raw command arguments to validate
     * @return true if input is valid, false otherwise
     */
    private boolean isValidCommandInput(final String[] rawArgs) {
        return rawArgs != null && rawArgs.length >= MINIMUM_COMMAND_ARGS;
    }

    /**
     * Handles unknown command scenarios.
     * 
     * @param commandName         the unknown command name
     * @param isPropagatedCommand whether this is a propagated command
     * @return error response buffer or null for propagated commands
     */
    private ByteBuffer handleUnknownCommand(final String commandName, final boolean isPropagatedCommand) {
        if (isPropagatedCommand) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignoring unknown propagated command: {}", commandName);
            }
            return null;
        }
        return ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.format(commandName));
    }

    /**
     * Handles command validation failures.
     * 
     * @param isPropagatedCommand whether this is a propagated command
     * @return error response buffer or null for propagated commands
     */
    private ByteBuffer handleValidationFailure(final boolean isPropagatedCommand) {
        if (isPropagatedCommand) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignoring invalid propagated command");
            }
            return null;
        }
        return ResponseBuilder.error(ErrorCode.WRONG_ARG_COUNT.getMessage());
    }

    private boolean canExecuteInCurrentMode(final SocketChannel clientChannel, final Command command) {
        final boolean inPubSub = pubSubManager.isInPubSubMode(clientChannel);
        return !inPubSub || isPubSubCommand(command);
    }

    private ByteBuffer executeCommand(final Command command,
            final CommandContext context,
            final SocketChannel clientChannel,
            final boolean isPropagatedCommand,
            final String[] rawArgs) {

        if (shouldQueueForTransaction(clientChannel, command, context)) {
            return ResponseCache.QUEUED_RESPONSE.duplicate();
        }

        // For read commands, skip timing overhead for maximum performance
        if (command.isReadCommand()) {
            final CommandResult result = command.execute(context);
            recordLightweightMetrics(command, result, isPropagatedCommand);
            return buildResponse(result);
        }

        // Only time write commands and others that need detailed metrics
        final Instant startTime = Instant.now();
        final CommandResult result = command.execute(context);
        final Duration executionTime = Duration.between(startTime, Instant.now());

        recordCommandMetrics(command, context, result, executionTime, isPropagatedCommand);
        handleAofLogging(result, command, isPropagatedCommand, context, rawArgs);

        final boolean shouldSendResponse = !isPropagatedCommand || command.isReplicationCommand();
        return shouldSendResponse ? buildResponse(result) : null;
    }

    private boolean shouldQueueForTransaction(final SocketChannel clientChannel, final Command command,
            final CommandContext context) {
        final TransactionState transactionState = transactionManager.getOrCreateState(clientChannel);
        if (shouldQueueInTransaction(transactionState, command)) {
            transactionState.queueCommand(command, context);
            return true;
        }
        return false;
    }

    private void handleAofLogging(final CommandResult result, final Command command, final boolean isPropagatedCommand,
            final CommandContext context, final String[] rawArgs) {
        if (result instanceof CommandResult.Success && shouldLogToAof(command, isPropagatedCommand) &&
                context.getServerContext().isAofMode()) {
            final var aofRepo = context.getServerContext().getAofRepository();
            if (aofRepo != null) {
                aofRepo.appendCommand(rawArgs);
            }
        }
    }

    private boolean shouldQueueInTransaction(final TransactionState state, final Command command) {
        return state.isInTransaction() && !isTransactionControlCommand(command);
    }

    private boolean isTransactionControlCommand(final Command command) {
        return command instanceof MultiCommand ||
                command instanceof ExecCommand ||
                command instanceof DiscardCommand;
    }

    private boolean isPubSubCommand(final Command command) {
        return command.isPubSubCommand() || command instanceof PingCommand;
    }

    private boolean shouldLogToAof(final Command command, final boolean isPropagatedCommand) {
        return command.isWriteCommand() &&
                !isPropagatedCommand &&
                !command.isPubSubCommand() &&
                !command.isReplicationCommand() &&
                !isTransactionControlCommand(command);
    }

    private ByteBuffer buildResponse(final CommandResult result) {
        return switch (result) {
            case CommandResult.MultiSuccess(final var responses) -> ResponseBuilder.merge(responses);
            case CommandResult.Success(final var response) -> response;
            case CommandResult.Error(final var message) -> ResponseBuilder.error(message);
            case CommandResult.Async() -> null; // No immediate response for async commands
        };
    }

    /**
     * Record lightweight metrics for read commands (no timing overhead).
     */
    private void recordLightweightMetrics(final Command command, final CommandResult result,
            final boolean isPropagatedCommand) {
        if (isPropagatedCommand) {
            return;
        }

        final var metricsCollector = this.context.getMetricsCollector();
        metricsCollector.recordReadResponse();

        // Record errors
        if (result instanceof CommandResult.Error) {
            metricsCollector.recordError();
        }
    }

    /**
     * Record Redis Enterprise compatible metrics for command execution.
     */
    private void recordCommandMetrics(final Command command, final CommandContext context,
            final CommandResult result, final Duration executionTime,
            final boolean isPropagatedCommand) {
        if (isPropagatedCommand) {
            return; // Don't double-count replicated commands
        }

        final var metricsCollector = this.context.getMetricsCollector();
        final String commandName = context.getArgs()[0].toUpperCase();

        // Lightweight metrics - only essential ones for read commands
        if (command.isReadCommand()) {
            metricsCollector.recordReadResponse();
        } else if (command.isWriteCommand()) {
            metricsCollector.recordWriteCommand(commandName, executionTime);
            metricsCollector.recordWriteResponse();
        } else {
            metricsCollector.recordOtherCommand(commandName, executionTime);
            metricsCollector.recordOtherResponse();
        }

        // Record errors
        if (result instanceof CommandResult.Error) {
            metricsCollector.recordError();
        }

        // Record replication metrics if this is a master
        if (command.isWriteCommand() && !context.getServerContext().getConfig().isReplicaMode()) {
            final var replicationManager = context.getServerContext().getReplicationManager();
            if (replicationManager.hasConnectedReplicas()) {
                metricsCollector.recordReplicationCommand();
            }
        }
    }
}