package errors;

public sealed interface ServerError
        permits ValidationError, StorageError, ProtocolError {
    String message();

    ErrorCode code();
}





