package commands.impl.config;

import java.util.List;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class ConfigCommand extends ReadCommand {
    @Override
    public String getName() {
        return "CONFIG";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        ValidationResult argCountValidation = CommandValidator.validateArgRange(context, 2, 3);
        if (!argCountValidation.isValid()) {
            return argCountValidation;
        }
        if (!"GET".equalsIgnoreCase(context.getArg(1))) {
            return ValidationResult.invalid("Only CONFIG GET is supported");
        }
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String parameter = context.getArg(2).toLowerCase();
        var config = context.getServerContext().getConfig();

        return config.getConfigParameter(parameter)
                .map(value -> CommandResult.success(ResponseBuilder.array(List.of(parameter, value))))
                .orElseGet(() -> CommandResult.error("Unknown configuration parameter: " + parameter));
    }
}