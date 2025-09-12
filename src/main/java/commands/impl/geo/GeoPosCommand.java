package commands.impl.geo;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import utils.GeoUtils;

public class GeoPosCommand extends ReadCommand {

    private static final String COMMAND_NAME = "GEOPOS";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        // At least: GEOPOS key member [member ...]
        return CommandValidator.minArgs(3).validate(context);
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();
        final List<String> members = context.getSlice(2, context.getArgCount());

        final Map<String, double[]> memberCoords = storage.geoPos(key, members);

        // Produce a list of ByteBuffers for RESP
        final List<ByteBuffer> respList = GeoUtils.formatGeoPosForResp(members, memberCoords);

        return CommandResult.success(ResponseBuilder.arrayOfBuffers(respList));
    }

}
