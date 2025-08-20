package commands;

import java.nio.ByteBuffer;

public sealed interface CommandResult
        permits CommandResult.Success, CommandResult.Error, CommandResult.Async {

    record Success(ByteBuffer response) implements CommandResult {
    }
    record Error(String message) implements CommandResult {
    }
    record Async() implements CommandResult {
    } // For blocking commands
}
