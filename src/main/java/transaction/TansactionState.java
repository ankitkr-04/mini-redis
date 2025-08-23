package transaction;

import java.util.ArrayList;
import java.util.List;
import commands.Command;
import commands.CommandArgs;
import errors.ErrorCode;

public final class TansactionState {
    private boolean inTransaction = false;
    private final List<QueuedCommand> queuedCommands = new ArrayList<>();

    public record QueuedCommand(Command command, CommandArgs args) {

    }

    public TansactionState() {
        this.inTransaction = false;
    }

    public void beginTransaction() {
        this.inTransaction = true;
        this.queuedCommands.clear();
    }

    public void queueCommand(Command command, CommandArgs args) {
        if (!inTransaction) {
            throw new IllegalStateException(ErrorCode.NOT_IN_TRANSACTION.getMessage());
        }

        QueuedCommand queuedCommand = new QueuedCommand(command, args);
        this.queuedCommands.add(queuedCommand);
    }

    public List<QueuedCommand> getQueuedCommands() {
        return new ArrayList<>(queuedCommands);
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void clearTransaction() {
        this.inTransaction = false;
        this.queuedCommands.clear();
    }

}
