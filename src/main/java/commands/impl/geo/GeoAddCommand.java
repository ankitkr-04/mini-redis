package commands.impl.geo;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import storage.expiry.ExpiryPolicy;
import utils.GeoUtils;

public class GeoAddCommand extends WriteCommand {

    private static final String COMMAND_NAME = "GEOADD";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        // at least: GEOADD key lon lat member
        return CommandValidator.validateCoordinates(2).validate(context);
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {

        int added = 0;
        for (final GeoUtils.GeoEntry e : GeoUtils.parseGeoEntries(context.getArgs())) {
            final boolean ok = context.getStorageService().geoAdd(context.getKey(), e.lon(), e.lat(), e.member(),
                    ExpiryPolicy.never());
            if (ok)
                added++;
        }

        return CommandResult.success(ResponseBuilder.integer(added));
    }
}
