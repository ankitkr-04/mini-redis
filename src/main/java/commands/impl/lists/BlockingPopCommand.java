package commands.impl.lists;

import java.util.List;
import java.util.Optional;

import blocking.BlockingManager;
import commands.base.BlockingCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class BlockingPopCommand extends BlockingCommand {
    private final BlockingManager blockingManager;

    public BlockingPopCommand(BlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String getName() {
        return "BLPOP";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        ValidationResult res = CommandValidator.validateArgRange(context, 3, Integer.MAX_VALUE);
        if (!res.isValid()) {
            return res;
        }
        return CommandValidator.validateTimeout(context.getArg(context.getArgCount() - 1));
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        List<String> keys = context.getSlice(1, context.getArgCount() - 1);
        String timeoutStr = context.getArg(context.getArgCount() - 1);
        double timeoutSec = Double.parseDouble(timeoutStr);
        long timeoutMs = (long) (timeoutSec * 1000);
        Optional<Long> optTimeout = timeoutSec == 0 ? Optional.empty() : Optional.of(timeoutMs);

        // Immediate check
        for (String key : keys) {
            if (context.getStorageService().getListLength(key) > 0) {
                String value = context.getStorageService().leftPop(key).orElse(null);
                return CommandResult.success(ResponseBuilder.array(List.of(key, value)));
            }
        }

        // Block
        blockingManager.blockClientForLists(keys, context.getClientChannel(), optTimeout);
        return CommandResult.async();
    }
}