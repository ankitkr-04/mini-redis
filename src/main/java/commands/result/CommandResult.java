package commands.result;

import java.nio.ByteBuffer;

public sealed interface CommandResult
        permits CommandResult.Success, CommandResult.Error, CommandResult.Async {

    static CommandResult success(ByteBuffer response) {
        return new Success(response);
    }

    static CommandResult error(String message) {
        return new Error(message);
    }

    static CommandResult async() {
        return new Async();
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isError() {
        return this instanceof Error;
    }

    default boolean isAsync() {
        return this instanceof Async;
    }

    record Success(ByteBuffer response) implements CommandResult {
    }

    record Error(String message) implements CommandResult {
    }

    record Async() implements CommandResult {
    }
}
