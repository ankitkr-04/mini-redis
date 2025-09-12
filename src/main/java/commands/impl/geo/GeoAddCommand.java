package commands.impl.geo;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import storage.expiry.ExpiryPolicy;
import utils.GeoUtils;

/**
 * Implementation of Redis GEOADD command.
 * 
 * Syntax: GEOADD key longitude latitude member [longitude latitude member ...]
 * 
 * Adds one or more geospatial members to the specified key.
 * Returns the number of elements added to the set.
 */
public class GeoAddCommand extends WriteCommand {

    private static final String COMMAND_NAME = "GEOADD";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        // Require at least: GEOADD key longitude latitude member
        return CommandValidator.validateCoordinates(2).validate(context);
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();
        
        int addedCount = 0;
        
        try {
            // Parse all coordinate/member triplets from the command
            for (final GeoUtils.GeoEntry entry : GeoUtils.parseGeoEntries(context.getArgs())) {
                final boolean wasAdded = storage.geoAdd(
                    key, 
                    entry.longitude(), 
                    entry.latitude(), 
                    entry.member(),
                    ExpiryPolicy.never());
                    
                if (wasAdded) {
                    addedCount++;
                }
            }
            
            return CommandResult.success(ResponseBuilder.integer(addedCount));
            
        } catch (final IllegalArgumentException e) {
            return CommandResult.error("Invalid coordinates: " + e.getMessage());
        }
    }
}
