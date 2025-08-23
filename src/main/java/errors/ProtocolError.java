package errors;

public record ProtocolError(String message, ErrorCode code) implements ServerError {
}
