package commands.impl.streams;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import storage.expiry.ExpiryPolicy;

/**
 * Implements the XADD command for adding entries to a stream.
 * <p>
 * Validates arguments, constructs the field-value map, and adds a new entry to
 * the specified stream.
 * Returns the entry ID on success or an error message on failure.
 * </p>
 */
public final class AddStreamCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddStreamCommand.class);

    private static final String COMMAND_NAME = "XADD";
    private static final int MIN_ARGUMENTS = 4;
    private static final int STREAM_ID_INDEX = 2;
    private static final int FIELD_START_INDEX = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.minArgs(MIN_ARGUMENTS).and(
                CommandValidator.evenArgCount()).and(
                        CommandValidator.streamIdArg(STREAM_ID_INDEX))
                .validate(context);

    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String streamKey = context.getKey();
        String streamId = context.getArg(STREAM_ID_INDEX);
        Map<String, String> fieldValueMap = context.getFieldValueMap(FIELD_START_INDEX);

        try {
            String entryId = context.getStorageService().addStreamEntry(
                    streamKey, streamId, fieldValueMap, ExpiryPolicy.never());
            publishDataAdded(streamKey, context.getServerContext());
            propagateCommand(context.getArgs(), context.getServerContext());

            LOGGER.debug("Stream entry added: key={}, id={}", streamKey, entryId);
            return CommandResult.success(ResponseBuilder.bulkString(entryId));
        } catch (IllegalArgumentException e) {
            LOGGER.info("Failed to add stream entry: {}", e.getMessage());
            return CommandResult.error(e.getMessage());
        }
    }
}