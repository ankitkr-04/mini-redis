package commands.impl.geo;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import utils.GeoUtils;

public class GeoDistCommand extends ReadCommand {

    private static final String COMMAND_NAME = "GEODIST";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {
        return CommandValidator.argRange(4, 5).validate(context); // GEODIST key member1 member2 [unit]
    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final var storage = context.getStorageService();
        final String key = context.getKey();
        final String member1 = context.getArg(2);
        final String member2 = context.getArg(3);
        final String unitStr = context.getArgCount() >= 5 ? context.getArg(4) : "m";

        final GeoUtils.GEO_UNIT unit = GeoUtils.parseUnitOrNull(unitStr);
        if (unit == null)
            return CommandResult.success(ResponseBuilder.bulkString(null));

        final Double dist = storage.geoDist(key, member1, member2, unit);
        return CommandResult.success(dist == null
                ? ResponseBuilder.bulkString(null)
                : ResponseBuilder.bulkString(String.format("%.6f", dist)));
    }
}
