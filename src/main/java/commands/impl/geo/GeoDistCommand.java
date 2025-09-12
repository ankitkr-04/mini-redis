package commands.impl.geo;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import utils.GeoUtils;

/**
 * Implementation of Redis GEODIST command.
 * 
 * Syntax: GEODIST key member1 member2 [unit]
 * 
 * Calculates the distance between two members in a geospatial set.
 * Supported units: m (meters), km (kilometers), mi (miles), ft (feet)
 * Default unit is meters if not specified.
 */
public class GeoDistCommand extends ReadCommand {

    private static final String COMMAND_NAME = "GEODIST";
    private static final String DEFAULT_UNIT = "m";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        // GEODIST key member1 member2 [unit]
        return CommandValidator.argRange(4, 5).validate(context);
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();
        final String member1 = context.getArg(2);
        final String member2 = context.getArg(3);
        
        // Use default unit if not specified
        final String unitString = context.getArgCount() >= 5 ? context.getArg(4) : DEFAULT_UNIT;

        // Parse and validate the unit
        final GeoUtils.GeoUnit unit = GeoUtils.GeoUnit.fromString(unitString);
        if (unit == null) {
            return CommandResult.error("Invalid unit: " + unitString);
        }

        try {
            // Calculate distance between the two members
            final Double distance = storage.geoDist(key, member1, member2, unit);
            
            if (distance == null) {
                // One or both members don't exist
                return CommandResult.success(ResponseBuilder.bulkString(null));
            }
            
            // Format distance with appropriate precision
            return CommandResult.success(ResponseBuilder.bulkString(String.format("%.6f", distance)));
            
        } catch (final IllegalArgumentException e) {
            return CommandResult.error("Error calculating distance: " + e.getMessage());
        }
    }
}
