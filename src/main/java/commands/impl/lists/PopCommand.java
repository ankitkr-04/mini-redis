package commands.impl.lists;

import java.util.List;
import java.util.Optional;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import storage.types.PopDirection;

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
        PopDirection direction = getPopDirection(context.getOperation());

        if (context.getArgCount() == 2) {
            Optional<String> value = popSingle(context, key, direction);
            return CommandResult.success(ResponseBuilder.bulkString(value.orElse(null)));
        } else {
            int count = Integer.parseInt(context.getArg(2));
            List<String> values = popMultiple(context, key, direction, count);
            return CommandResult.success(ResponseBuilder.array(values));
        }
    }

    private PopDirection getPopDirection(String operation) {
        return "LPOP".equalsIgnoreCase(operation) ? PopDirection.LEFT : PopDirection.RIGHT;
    }

    private Optional<String> popSingle(CommandContext context, String key, PopDirection direction) {
        return direction.isLeft()
                ? context.getStorageService().leftPop(key)
                : context.getStorageService().rightPop(key);
    }

    private List<String> popMultiple(CommandContext context, String key, PopDirection direction, int count) {
        return direction.isLeft()
                ? context.getStorageService().leftPop(key, count)
                : context.getStorageService().rightPop(key, count);
    }
}