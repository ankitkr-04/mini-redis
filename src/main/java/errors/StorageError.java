package errors;

public record StorageError(String message, ErrorCode code) implements ServerError {
}

