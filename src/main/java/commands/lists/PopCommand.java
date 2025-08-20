package commands.lists;

import java.util.List;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import common.ValidationUtil;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class PopCommand implements Command {
    @Override
    public String name() {
        return "POP"; // Handles LPOP/RPOP
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
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
            return new CommandResult.Success(ResponseWriter.bulkString(value.orElse(null)));
        } else {
            // Multi pop
            int count = Integer.parseInt(args.arg(2));
            List<String> values = switch (operation) {
                case "LPOP" -> storage.leftPop(key, count);
                case "RPOP" -> storage.rightPop(key, count);
                default -> throw new IllegalArgumentException(
                        "Unsupported operation: " + operation);
            };
            return new CommandResult.Success(ResponseWriter.array(values));
        }
    }

    @Override
    public boolean validate(CommandArgs args) {
        if (args.argCount() == 2) {
            return true;
        } else if (args.argCount() == 3) {
            return ValidationUtil.isValidInteger(args.arg(2));
        }
        return false;
    }
}
