package transaction;

import java.util.ArrayList;
import java.util.List;

import commands.context.CommandContext;
import commands.core.Command;
import errors.ErrorCode;

public final class TransactionState {
    private boolean inTransaction = false;
    private final List<QueuedCommand> queuedCommands = new ArrayList<>();

    public record QueuedCommand(Command command, String operation, String[] rawArgs) {
    }

    public void beginTransaction() {
        this.inTransaction = true;
        this.queuedCommands.clear();
    }

    public void queueCommand(Command command, CommandContext context) {
        if (!inTransaction) {
            throw new IllegalStateException(ErrorCode.NOT_IN_TRANSACTION.getMessage());
        }
        this.queuedCommands.add(new QueuedCommand(command, context.getOperation(), context.getArgs()));
    }

    public List<QueuedCommand> getQueuedCommands() {
        return List.copyOf(queuedCommands);
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void clearTransaction() {
        this.inTransaction = false;
        this.queuedCommands.clear();
    }
}