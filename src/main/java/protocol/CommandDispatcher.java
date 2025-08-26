package protocol;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;

import commands.context.CommandContext;
import commands.core.Command;
import commands.impl.basic.PingCommand;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import commands.registry.CommandRegistry;
import commands.result.CommandResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import pubsub.PubSubManager;
import server.ServerContext;
import storage.StorageService;
import transaction.TransactionManager;
import transaction.TransactionState;

public final class CommandDispatcher {
    private final CommandRegistry registry;
    private final StorageService storage;
    private final TransactionManager transactionManager;
    private final PubSubManager pubSubManager;
    private final ServerContext context;

    public CommandDispatcher(CommandRegistry registry,
            StorageService storage,
            TransactionManager transactionManager,
            PubSubManager pubSubManager,
            ServerContext context) {
        this.registry = registry;
        this.storage = storage;
        this.pubSubManager = pubSubManager;
        this.transactionManager = transactionManager;
        this.context = context;
    }

    public ByteBuffer dispatch(String[] rawArgs, SocketChannel clientChannel) {
        return dispatch(rawArgs, clientChannel, false);
    }

    public ByteBuffer dispatch(String[] rawArgs,
            SocketChannel clientChannel,
            boolean isPropagatedCommand) {
        if (!isValidCommandInput(rawArgs)) {
            return ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.getMessage());
        }

        String commandName = rawArgs[0].toUpperCase();
        Command command = registry.getCommand(commandName);

        if (command == null) {
            return handleUnknownCommand(commandName, isPropagatedCommand);
        }

        CommandContext cmdContext = new CommandContext(commandName, rawArgs, clientChannel, storage, context);

        if (!command.validate(cmdContext)) {
            return handleValidationFailure(isPropagatedCommand);
        }

        if (!canExecuteInCurrentMode(clientChannel, command)) {
            return ResponseBuilder.error(ErrorCode.NOT_ALLOWED_IN_PUBSUB_MODE.format(commandName.toLowerCase()));
        }

        return executeCommand(command, cmdContext, clientChannel, isPropagatedCommand, rawArgs);
    }

    private boolean isValidCommandInput(String[] rawArgs) {
        return rawArgs != null && rawArgs.length > 0;
    }

    private ByteBuffer handleUnknownCommand(String commandName, boolean isPropagatedCommand) {
        return isPropagatedCommand
                ? null
                : ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.format(commandName));
    }

    private ByteBuffer handleValidationFailure(boolean isPropagatedCommand) {
        return isPropagatedCommand
                ? null
                : ResponseBuilder.error(ErrorCode.WRONG_ARG_COUNT.getMessage());
    }

    private boolean canExecuteInCurrentMode(SocketChannel clientChannel, Command command) {
        boolean inPubSub = pubSubManager.isInPubSubMode(clientChannel);
        return !inPubSub || isPubSubCommand(command);
    }

    private ByteBuffer executeCommand(Command command,
            CommandContext context,
            SocketChannel clientChannel,
            boolean isPropagatedCommand,
            String[] rawArgs) {

        // Start timing for latency measurement
        Instant startTime = Instant.now();

        if (shouldQueueForTransaction(clientChannel, command, context)) {
            return ResponseBuilder.encode(ProtocolConstants.RESP_QUEUED);
        }

        // Execute the command
        CommandResult result = command.execute(context);

        // Calculate execution time
        Duration executionTime = Duration.between(startTime, Instant.now());

        // Record metrics based on command type and result
        recordCommandMetrics(command, context, result, executionTime, isPropagatedCommand);

        handleAofLogging(result, command, isPropagatedCommand, context, rawArgs);

        boolean shouldSendResponse = !isPropagatedCommand || command.isReplicationCommand();
        return shouldSendResponse ? buildResponse(result) : null;
    }

    private boolean shouldQueueForTransaction(SocketChannel clientChannel, Command command, CommandContext context) {
        TransactionState transactionState = transactionManager.getOrCreateState(clientChannel);
        if (shouldQueueInTransaction(transactionState, command)) {
            transactionState.queueCommand(command, context);
            return true;
        }
        return false;
    }

    private void handleAofLogging(CommandResult result, Command command, boolean isPropagatedCommand,
            CommandContext context, String[] rawArgs) {
        if (result instanceof CommandResult.Success && shouldLogToAof(command, isPropagatedCommand) &&
                context.getServerContext().isAofMode()) {
            var aofRepo = context.getServerContext().getAofRepository();
            if (aofRepo != null) {
                aofRepo.appendCommand(rawArgs);
            }
        }
    }

    private boolean shouldQueueInTransaction(TransactionState state, Command command) {
        return state.isInTransaction() && !isTransactionControlCommand(command);
    }

    private boolean isTransactionControlCommand(Command command) {
        return command instanceof MultiCommand ||
                command instanceof ExecCommand ||
                command instanceof DiscardCommand;
    }

    private boolean isPubSubCommand(Command command) {
        return command.isPubSubCommand() || command instanceof PingCommand;
    }

    private boolean shouldLogToAof(Command command, boolean isPropagatedCommand) {
        return command.isWriteCommand() &&
                !isPropagatedCommand &&
                !command.isPubSubCommand() &&
                !command.isReplicationCommand() &&
                !isTransactionControlCommand(command);
    }

    private ByteBuffer buildResponse(CommandResult result) {
        return switch (result) {
            case CommandResult.Success(var response) -> response;
            case CommandResult.Error(var message) -> ResponseBuilder.error(message);
            case CommandResult.Async() -> null; // No immediate response for async commands
        };
    }

    /**
     * Record Redis Enterprise compatible metrics for command execution.
     */
    private void recordCommandMetrics(Command command, CommandContext context,
            CommandResult result, Duration executionTime,
            boolean isPropagatedCommand) {
        if (isPropagatedCommand) {
            return; // Don't double-count replicated commands
        }

        var metricsCollector = this.context.getMetricsCollector();
        String commandName = context.getArgs()[0].toUpperCase();

        // Classify command type for Redis Enterprise metrics
        if (command.isReadCommand()) {
            metricsCollector.recordReadCommand(commandName, executionTime);
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
            var replicationManager = context.getServerContext().getReplicationManager();
            if (replicationManager.hasConnectedReplicas()) {
                metricsCollector.recordReplicationCommand();
            }
        }
    }
}