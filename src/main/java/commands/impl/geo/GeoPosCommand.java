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

/**
 * Implementation of Redis GEOPOS command.
 * 
 * Syntax: GEOPOS key member [member ...]
 * 
 * Returns longitude and latitude coordinates for the specified members.
 * For non-existing members, null values are returned in the response array.
 */
public class GeoPosCommand extends ReadCommand {

    private static final String COMMAND_NAME = "GEOPOS";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        // Require at least: GEOPOS key member [member ...]
        return CommandValidator.minArgs(3).validate(context);
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();
        
        // Extract member names from arguments (starting from index 2)
        final List<String> members = context.getSlice(2, context.getArgCount());

        try {
            // Get coordinates for all requested members
            final Map<String, double[]> memberCoordinates = storage.geoPos(key, members);

            // Format response as RESP protocol arrays
            final List<ByteBuffer> responseList = GeoUtils.formatGeoPosResponse(members, memberCoordinates);

            return CommandResult.success(ResponseBuilder.arrayOfBuffers(responseList));
            
        } catch (final Exception e) {
            return CommandResult.error("Error retrieving positions: " + e.getMessage());
        }
    }
}
