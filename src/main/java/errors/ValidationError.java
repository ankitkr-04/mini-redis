package errors;

public record ValidationError(String message, ErrorCode code) implements ServerError {
}

