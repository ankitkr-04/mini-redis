package commands.impl.geo;

import java.util.List;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import utils.GeoUtils;

public class GeoSearchCommand extends ReadCommand {

    private static final String COMMAND_NAME = "GEOSEARCH";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        return CommandValidator.argRange(5, 6).validate(context); // GEOSEARCH key lon lat radius [unit]
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();

        final double longitude = context.getDoubleArg(2);
        final double latitude = context.getDoubleArg(3);
        final double radius = context.getDoubleArg(4);
        final String unitStr = context.getArgCount() >= 6 ? context.getArg(5) : "m";

        final GeoUtils.GEO_UNIT unit = GeoUtils.parseUnitOrNull(unitStr);
        if (unit == null) return CommandResult.success(ResponseBuilder.bulkString(null));

        final double radiusMeters = GeoUtils.convertToMeters(radius, unit);
        final List<String> members = storage.geoSearch(key, longitude, latitude, radiusMeters);

        return CommandResult.success(ResponseBuilder.array(members));
    }
}
