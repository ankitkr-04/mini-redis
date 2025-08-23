package errors;

public record ServerError(String message, ErrorType type) {
    public enum ErrorType {
        VALIDATION, STORAGE, PROTOCOL, COMMAND
    }

    public static ServerError validation(String message) {
        return new ServerError(message, ErrorType.VALIDATION);
    }

    public static ServerError storage(String message) {
        return new ServerError(message, ErrorType.STORAGE);
    }

    public static ServerError command(String message) {
        return new ServerError(message, ErrorType.COMMAND);
    }
}
