package commands.impl.sortedsets;

import java.util.ArrayList;
import java.util.List;

import collections.QuickZSet;
import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class ZRangeCommand extends ReadCommand {

    @Override
    public String getName() {
        return "ZRANGE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // ZRANGE key start stop [WITHSCORES]

        // Validate minimum and maximum arguments
        ValidationResult argCountValidation = CommandValidator.validateArgRange(context, 4, 5);
        if (!argCountValidation.isValid())
            return argCountValidation;

        // Validate that start and stop are integers
        ValidationResult startValidation = CommandValidator.validateInteger(context.getArg(2));
        if (!startValidation.isValid())
            return startValidation;

        ValidationResult stopValidation = CommandValidator.validateInteger(context.getArg(3));
        if (!stopValidation.isValid())
            return stopValidation;

        // Optional WITHSCORES
        if (context.getArgCount() == 5 && !"WITHSCORES".equalsIgnoreCase(context.getArg(4))) {
            return ValidationResult.invalid("syntax error: expected 'WITHSCORES'");
        }

        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey(); // safer than getArg(1)
        int start = context.getIntArg(2); // parse with context helper
        int stop = context.getIntArg(3);
        boolean withScores = context.getArgCount() == 5 && "WITHSCORES".equalsIgnoreCase(context.getArg(4));

        var storage = context.getStorageService(); // directly from context
        List<QuickZSet.ZSetEntry> entries = storage.zRange(key, start, stop);

        List<String> result = new ArrayList<>();
        for (QuickZSet.ZSetEntry entry : entries) {
            result.add(entry.member());
            if (withScores) {
                result.add(String.valueOf(entry.score()));
            }
        }

        return CommandResult.success(ResponseBuilder.array(result));
    }

}
