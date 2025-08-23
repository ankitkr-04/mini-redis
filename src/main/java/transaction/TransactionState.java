package transaction;

import java.util.ArrayList;
import java.util.List;
import commands.Command;
import commands.CommandArgs;
import errors.ErrorCode;

public final class TransactionState {
    private boolean inTransaction = false;
    private final List<QueuedCommand> queuedCommands = new ArrayList<>();

    public record QueuedCommand(Command command, CommandArgs args) {
    }

    public void beginTransaction() {
        this.inTransaction = true;
        this.queuedCommands.clear();
    }

    public void queueCommand(Command command, CommandArgs args) {
        if (!inTransaction) {
            throw new IllegalStateException(ErrorCode.NOT_IN_TRANSACTION.getMessage());
        }
        this.queuedCommands.add(new QueuedCommand(command, args));
    }

    public List<QueuedCommand> getQueuedCommands() {
        return List.copyOf(queuedCommands); // Java 10+ immutable copy
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void clearTransaction() {
        this.inTransaction = false;
        this.queuedCommands.clear();
    }
}
