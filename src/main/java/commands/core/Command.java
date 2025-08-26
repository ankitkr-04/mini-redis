package commands.core;

import commands.context.CommandContext;
import commands.result.CommandResult;

/**
 * Represents a command that can be executed within the system.
 * Provides methods for command execution, validation, and command type checks.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public interface Command {

    /**
     * Returns the name of the command.
     *
     * @return the command name
     */
    String getName();

    /**
     * Executes the command with the given context.
     *
     * @param context the command context
     * @return the result of command execution
     */
    CommandResult execute(CommandContext context);

    /**
     * Validates the command with the given context.
     *
     * @param context the command context
     * @return true if the command is valid, false otherwise
     */
    boolean validate(CommandContext context);

    /**
     * Indicates if the command requires a client connection.
     *
     * @return true if client is required, false otherwise
     */
    default boolean requiresClient() {
        return false;
    }

    /**
     * Indicates if the command is a write operation.
     *
     * @return true if it is a write command, false otherwise
     */
    default boolean isWriteCommand() {
        return false;
    }

    /**
     * Indicates if the command is a read operation.
     *
     * @return true if it is a read command, false otherwise
     */
    default boolean isReadCommand() {
        return !isWriteCommand() && !isPubSubCommand() && !isReplicationCommand();
    }

    /**
     * Indicates if the command is related to replication.
     *
     * @return true if it is a replication command, false otherwise
     */
    default boolean isReplicationCommand() {
        return false;
    }

    /**
     * Indicates if the command is related to publish/subscribe.
     *
     * @return true if it is a pub/sub command, false otherwise
     */
    default boolean isPubSubCommand() {
        return false;
    }

    /**
     * Indicates if the command is a blocking operation.
     *
     * @return true if it is a blocking command, false otherwise
     */
    default boolean isBlockingCommand() {
        return false;
    }
}