package commands.impl.lists;

import java.util.List;
import java.util.Optional;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class PopCommand extends WriteCommand {
    @Override
    public String getName() {
        return "POP"; // Will handle LPOP/RPOP
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgRange(context, 2, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        boolean isLeft = "LPOP".equalsIgnoreCase(context.getOperation());

        if (context.getArgCount() == 2) {
            Optional<String> value = isLeft
                    ? context.getStorageService().leftPop(key)
                    : context.getStorageService().rightPop(key);
            return CommandResult.success(ResponseBuilder.bulkString(value.orElse(null)));
        } else {
            int count = Integer.parseInt(context.getArg(2));
            List<String> values = isLeft
                    ? context.getStorageService().leftPop(key, count)
                    : context.getStorageService().rightPop(key, count);
            return CommandResult.success(ResponseBuilder.array(values));
        }
    }
}