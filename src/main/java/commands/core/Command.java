package commands.core;

import commands.context.CommandContext;
import commands.result.CommandResult;

public interface Command {
    String getName();

    CommandResult execute(CommandContext context);

    boolean validate(CommandContext context);

    default boolean requiresClient() {
        return false;
    }

    default boolean isWriteCommand() {
        return false;
    }

    default boolean isReadCommand() {
        return !isWriteCommand() && !isPubSubCommand() && !isReplicationCommand();
    }

    default boolean isReplicationCommand() {
        return false;
    }

    default boolean isPubSubCommand() {
        return false;
    }

    default boolean isBlockingCommand() {
        return false;
    }
}