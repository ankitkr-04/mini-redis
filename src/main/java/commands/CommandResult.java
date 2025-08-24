package commands;

import java.nio.ByteBuffer;

public sealed interface CommandResult
        permits CommandResult.Success, CommandResult.Error, CommandResult.Async {

    public static CommandResult success(ByteBuffer response) {
        return new Success(response);
    }

    public static CommandResult error(String message) {
        return new Error(message);
    }

    public static CommandResult async() {
        return new Async();
    }

    public default boolean isSuccess() {
        return this instanceof Success;
    }

    record Success(ByteBuffer response) implements CommandResult {
    }

    record Error(String message) implements CommandResult {
    }

    record Async() implements CommandResult {
    } // For blocking commands
}
