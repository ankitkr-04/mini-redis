package validation;

import java.util.Optional;

import errors.ServerError;

public sealed interface ValidationResult permits ValidationResult.Valid, ValidationResult.Invalid {
    boolean isValid();

    Optional<ServerError> error();

    record Valid() implements ValidationResult {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Optional<ServerError> error() {
            return Optional.empty();
        }
    }

    record Invalid(ServerError cause) implements ValidationResult {
        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Optional<ServerError> error() {
            return Optional.of(cause);
        }
    }

    static ValidationResult valid() {
        return new Valid();
    }

    static ValidationResult invalid(ServerError error) {
        return new Invalid(error);
    }

    static ValidationResult invalid(String message) {
        return new Invalid(ServerError.validation(message));
    }
}
