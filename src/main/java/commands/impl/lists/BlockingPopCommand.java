package commands.impl.lists;

import java.util.List;
import java.util.Optional;
import blocking.BlockingManager;
import commands.CommandArgs;
import commands.CommandResult;
import commands.base.BlockingCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class BlockingPopCommand extends BlockingCommand {
    private final BlockingManager blockingManager;

    public BlockingPopCommand(BlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String name() {
        return "BLPOP";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        var res = ValidationUtils.validateArgRange(args, 3, Integer.MAX_VALUE);
        if (!res.isValid())
            return res;
        return ValidationUtils.validateTimeout(args.arg(args.argCount() - 1));
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        List<String> keys = args.slice(1, args.argCount() - 1);
        String timeoutStr = args.arg(args.argCount() - 1);
        double timeoutSec = Double.parseDouble(timeoutStr);
        long timeoutMs = (long) (timeoutSec * 1000);
        Optional<Long> optTimeout = timeoutSec == 0 ? Optional.empty() : Optional.of(timeoutMs);

        // Immediate check
        for (String key : keys) {
            if (storage.getListLength(key) > 0) {
                String value = storage.leftPop(key).orElse(null);
                return new CommandResult.Success(ResponseBuilder.array(List.of(key, value)));
            }
        }

        // Block
        blockingManager.blockClientForLists(keys, args.clientChannel(), optTimeout);
        return new CommandResult.Async();
    }
}
