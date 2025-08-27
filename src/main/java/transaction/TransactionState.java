package transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import commands.context.CommandContext;
import commands.core.Command;

/**
 * Manages the state of a Redis transaction (MULTI/EXEC), including queued
 * commands and watched keys.
 * <p>
 * Provides methods to begin, queue, clear, and invalidate transactions, as well
 * as manage watched keys.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */

public final class TransactionState {

    /**
     * Error message for operations attempted outside a transaction.
     */
    private static final String ERROR_NOT_IN_TRANSACTION = "Operation not allowed outside of a transaction.";

    private boolean inTransaction = false;
    private final List<QueuedCommand> queuedCommands = new ArrayList<>();
    private final Set<String> watchedKeys = new HashSet<>();
    private boolean transactionInvalid = false;

    /**
     * Represents a command queued during a transaction.
     * 
     * @param command   The command object
     * @param operation The operation name
     * @param rawArgs   The raw arguments
     */
    public record QueuedCommand(Command command, String operation, String[] rawArgs) {

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof QueuedCommand other))
                return false;
            return command.equals(other.command)
                    && operation.equals(other.operation)
                    && java.util.Arrays.equals(rawArgs, other.rawArgs);
        }

        @Override
        public int hashCode() {
            int result = command.hashCode();
            result = 31 * result + operation.hashCode();
            result = 31 * result + java.util.Arrays.hashCode(rawArgs);
            return result;
        }

        @Override
        public String toString() {
            return "QueuedCommand{" +
                    "command=" + command +
                    ", operation='" + operation + '\'' +
                    ", rawArgs=" + java.util.Arrays.toString(rawArgs) +
                    '}';
        }
    }

    /**
     * Begins a new transaction, clearing any previously queued commands.
     */
    public void beginTransaction() {
        this.inTransaction = true;
        this.queuedCommands.clear();
    }

    /**
     * Queues a command for execution in the current transaction.
     * 
     * @param command The command to queue
     * @param context The command context
     * @throws IllegalStateException if not in a transaction
     */
    public void queueCommand(Command command, CommandContext context) {
        if (!inTransaction) {
            // Use constant for error message
            throw new IllegalStateException(ERROR_NOT_IN_TRANSACTION);
        }
        this.queuedCommands.add(new QueuedCommand(command, context.getOperation(), context.getArgs()));
    }

    public List<QueuedCommand> getQueuedCommands() {
        return List.copyOf(queuedCommands);
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    /**
     * Clears the current transaction state, including queued commands and
     * invalidation flag.
     */
    public void clearTransaction() {
        this.inTransaction = false;
        this.queuedCommands.clear();
        this.transactionInvalid = false;
    }

    /**
     * Adds a key to the set of watched keys for this transaction.
     * 
     * @param key The key to watch
     */
    public void addWatchedKey(String key) {
        watchedKeys.add(key);
    }

    /**
     * Clears all watched keys and resets the invalidation flag.
     */
    public void clearWatchedKeys() {
        watchedKeys.clear();
        transactionInvalid = false;
    }

    /**
     * Returns an unmodifiable view of the watched keys.
     * 
     * @return Set of watched keys
     */
    public Set<String> getWatchedKeys() {
        return Set.copyOf(watchedKeys);
    }

    /**
     * Checks if any keys are currently being watched.
     * 
     * @return true if there are watched keys, false otherwise
     */
    public boolean hasWatchedKeys() {
        return !watchedKeys.isEmpty();
    }

    /**
     * Marks the transaction as invalid (e.g., due to a watched key change).
     */
    public void invalidateTransaction() {
        transactionInvalid = true;
    }

    /**
     * Checks if the transaction is currently invalid.
     * 
     * @return true if invalid, false otherwise
     */
    public boolean isTransactionInvalid() {
        return transactionInvalid;
    }

    /**
     * Checks if a specific key is being watched.
     * 
     * @param key The key to check
     * @return true if the key is watched, false otherwise
     */
    public boolean isKeyWatched(String key) {
        return watchedKeys.contains(key);
    }
}