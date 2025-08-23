package commands;

import storage.StorageService;

public interface Command {
    String name();
    CommandResult execute(CommandArgs args, StorageService storage);
    boolean validate(CommandArgs args);
    
    default boolean requiresClient() {
        return false;
    }
}