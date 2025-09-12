package commands.impl.geo;

import java.util.List;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import utils.GeoUtils;

/**
 * Implementation of Redis GEOSEARCH command.
 * 
 * Supports the following syntax:
 * GEOSEARCH key FROMMEMBER member BYRADIUS radius unit
 * GEOSEARCH key FROMLONLAT longitude latitude BYRADIUS radius unit
 * 
 * Returns members within the specified radius from the given point.
 */
public class GeoSearchCommand extends ReadCommand {

    private static final String COMMAND_NAME = "GEOSEARCH";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        // GEOSEARCH requires at least 6 arguments: key FROM... BY...
        return CommandValidator.minArgs(6).validate(context);
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();

        try {
            // Parse the complex GEOSEARCH syntax using GeoUtils
            final GeoUtils.GeoSearchParams params = GeoUtils.parseGeoSearchCommand(context.getArgs());

            if (params == null) {
                return CommandResult.error("Invalid GEOSEARCH syntax");
            }

            // Resolve coordinates based on search type
            final double[] searchCoordinates = resolveSearchCoordinates(storage, key, params);
            
            if (searchCoordinates == null) {
                // Member not found or invalid coordinates
                return CommandResult.success(ResponseBuilder.array(List.of()));
            }

            // Perform the geospatial search
            final List<String> matchingMembers = storage.geoSearch(
                key, 
                searchCoordinates[0], 
                searchCoordinates[1], 
                params.radiusMeters());

            return CommandResult.success(ResponseBuilder.array(matchingMembers));

        } catch (final NumberFormatException e) {
            return CommandResult.error("Invalid numeric value in command");
        } catch (final IllegalArgumentException e) {
            return CommandResult.error("Invalid coordinates: " + e.getMessage());
        }
    }

    /**
     * Resolve search coordinates based on whether we're searching from a member or coordinates.
     */
    private double[] resolveSearchCoordinates(final storage.StorageService storage, 
                                            final String key,
                                            final GeoUtils.GeoSearchParams params) {
        if (params.isFromMember()) {
            // Search from existing member's position
            final var memberPositions = storage.geoPos(key, List.of(params.memberName()));
            return memberPositions.get(params.memberName());
        } else {
            // Search from explicit coordinates
            return new double[] { params.longitude(), params.latitude() };
        }
    }
}
