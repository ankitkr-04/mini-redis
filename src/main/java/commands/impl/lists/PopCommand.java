package commands.impl.lists;

import java.util.List;
import java.util.Optional;
import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class PopCommand extends ReadCommand {
    @Override
    public String name() {
        return "POP"; // LPOP/RPOP
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgRange(args, 2, 3);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String key = args.key();
        boolean isLeft = "LPOP".equalsIgnoreCase(args.operation());
        if (args.argCount() == 2) {
            Optional<String> value = isLeft ? storage.leftPop(key) : storage.rightPop(key);
            return new CommandResult.Success(ResponseBuilder.bulkString(value.orElse(null)));
        } else {
            int count = Integer.parseInt(args.arg(2));
            List<String> values =
                    isLeft ? storage.leftPop(key, count) : storage.rightPop(key, count);
            return new CommandResult.Success(ResponseBuilder.array(values));
        }
    }
}
