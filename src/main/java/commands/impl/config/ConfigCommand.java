package commands.impl.config;

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
        return CommandValidator.validateArgRange(context, 1, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var config = context.getServerContext().getConfig();
        String subCommand = context.getArg(1).toUpperCase();

        String parameter = context.getArgCount() >= 3 ? context.getArg(2) : null;

        // Support get only for now

        return config.getConfigParameter(parameter)
                .map(value -> CommandResult.success(ResponseBuilder.bulkString(value)))
                .orElseGet(() -> CommandResult.error("no such parameter"));

    }

}
