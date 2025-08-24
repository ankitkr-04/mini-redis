package commands.impl.basic;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import server.ServerInfo;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class InfoCommand extends ReadCommand {
    private final ServerInfo serverInfo;

    public InfoCommand(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public String name() {
        return "INFO";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgRange(args, 1, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String section = args.argCount() == 2 ? args.arg(1) : null;
        return new CommandResult.Success(serverInfo.getInfoResponse(section));
    }
}
