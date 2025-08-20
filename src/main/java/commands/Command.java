package commands;

import storage.interfaces.StorageEngine;

public interface Command {
    String name();

    CommandResult execute(CommandArgs args, StorageEngine storage);

    boolean validate(CommandArgs args);

    default boolean requiresClient() {
        return false; // Most commands don't need client channel
    }
}
