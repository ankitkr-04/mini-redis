package commands.impl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

public class FlushAllCommand extends WriteCommand {

    Logger LOGGER = LoggerFactory.getLogger(FlushAllCommand.class);

    private static final String COMMAND_NAME = "FLUSHALL";
    private static final int MIN_ARG_COUNT = 1;
    private static final int MAX_ARG_COUNT = 2; // Allowing optional SYNC/ASYNC argument
    private static final String SYNC = "SYNC";
    private static final String ASYNC = "ASYNC";
    private static final long DELAY_MILLIS = 0L;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argRange(MIN_ARG_COUNT, MAX_ARG_COUNT).and(
                CommandValidator.when(arg -> arg.getArgCount() == 2,
                        CommandValidator.argEquals(1, SYNC).or(CommandValidator.argEquals(1, ASYNC))))
                .validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        if (context.getArgCount() == 2 && ASYNC.equalsIgnoreCase(context.getArg(1))) {
            var scheduler = context.getServerContext().getTimeoutScheduler();
            scheduler.schedule(DELAY_MILLIS, () -> {
                try {
                    LOGGER.info("Async Flush Started");
                    context.getStorageService().clear();
                    LOGGER.info("Async Flush Finished");
                } catch (Exception e) {
                    LOGGER.error("Async Flush failed", e);
                }
            });

        } else {
            context.getStorageService().clear();
        }

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }

}
