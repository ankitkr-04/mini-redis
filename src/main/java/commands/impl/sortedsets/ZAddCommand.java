package commands.impl.sortedsets;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class ZAddCommand extends WriteCommand {
    @Override
    public String getName() {
        return "ZADD";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // ZADD key score member [score member ...]
        ValidationResult argCountValidation = CommandValidator.validateMinArgs(context, 4);
        if (!argCountValidation.isValid())
            return argCountValidation;

        // Ensure arguments after key come in pairs (score, member)
        int pairCount = context.getArgCount() - 2;
        if (pairCount % 2 != 0) {
            return ValidationResult.invalid("arguments after key must be in score-member pairs");
        }

        // Validate all scores
        for (int i = 2; i < context.getArgCount(); i += 2) {
            ValidationResult scoreValidation = CommandValidator.validateDouble(context.getArg(i));
            if (!scoreValidation.isValid())
                return scoreValidation;
        }

        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        var storage = context.getStorageService();

        int addedCount = 0;

        // Iterate over score-member pairs
        for (int i = 2; i < context.getArgCount(); i += 2) {
            double score = context.getDoubleArg(i);
            String member = context.getArg(i + 1);

            storage.zAdd(key, member, score);
            addedCount++;
        }

        return CommandResult.success(ResponseBuilder.integer(addedCount));
    }

}
