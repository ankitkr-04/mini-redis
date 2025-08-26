package commands.result;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the result of a command execution.
 * Provides factory methods for success, error, and async results.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public sealed interface CommandResult
        permits CommandResult.Success, CommandResult.Error, CommandResult.Async {

    Logger LOGGER = LoggerFactory.getLogger(CommandResult.class);

    /**
     * Creates a successful command result with the given response.
     *
     * @param response the response as a ByteBuffer
     * @return a Success result
     */
    static CommandResult success(ByteBuffer response) {
        if (response == null) {
            LOGGER.warn("Success response is null.");
        } else {
            LOGGER.debug("Success response created.");
        }
        return new Success(response);
    }

    /**
     * Creates an error command result with the given error message.
     *
     * @param errorMessage the error message
     * @return an Error result
     */
    static CommandResult error(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            LOGGER.warn("Error message is null or blank.");
        } else {
            LOGGER.info("Error result created: {}", errorMessage);
        }
        return new Error(errorMessage);
    }

    /**
     * Creates an async command result.
     *
     * @return an Async result
     */
    static CommandResult async() {
        LOGGER.trace("Async result created.");
        return new Async();
    }

    /**
     * Checks if the result is a success.
     *
     * @return true if success, false otherwise
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Checks if the result is an error.
     *
     * @return true if error, false otherwise
     */
    default boolean isError() {
        return this instanceof Error;
    }

    /**
     * Checks if the result is async.
     *
     * @return true if async, false otherwise
     */
    default boolean isAsync() {
        return this instanceof Async;
    }

    /**
     * Represents a successful command result.
     *
     * @param response the response as a ByteBuffer
     */
    record Success(ByteBuffer response) implements CommandResult {
    }

    /**
     * Represents an error command result.
     *
     * @param message the error message
     */
    record Error(String message) implements CommandResult {
    }

    /**
     * Represents an async command result.
     */
    record Async() implements CommandResult {
    }
}
