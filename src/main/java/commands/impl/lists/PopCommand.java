package commands.impl.lists;

import java.util.List;
import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class PopCommand extends ReadCommand {
    @Override
    public String name() {
        return "POP"; // Handles LPOP/RPOP
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        if (args.argCount() == 2) {
            return ValidationResult.valid();
        } else if (args.argCount() == 3) {
            return CommandValidator.validateInteger(args.arg(2));
        }
        return CommandValidator.validateArgRange(args, 2, 3);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String operation = args.operation().toUpperCase();
        String key = args.key();

        if (args.argCount() == 2) {
            // Single pop
            var value = switch (operation) {
                case "LPOP" -> storage.leftPop(key);
                case "RPOP" -> storage.rightPop(key);
                default -> throw new IllegalArgumentException(
                        "Unsupported operation: " + operation);
            };
            return new CommandResult.Success(ResponseBuilder.bulkString(value.orElse(null)));
        } else {
            // Multi pop
            int count = Integer.parseInt(args.arg(2));
            List<String> values = switch (operation) {
                case "LPOP" -> storage.leftPop(key, count);
                case "RPOP" -> storage.rightPop(key, count);
                default -> throw new IllegalArgumentException(
                        "Unsupported operation: " + operation);
            };
            return new CommandResult.Success(ResponseBuilder.array(values));
        }
    }
}
