package commands.impl.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Handles the Redis CONFIG GET command.
 * <p>
 * Only supports fetching configuration parameters using "CONFIG GET
 * &lt;parameter&gt;".
 * </p>
 */
public class ConfigCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCommand.class);

    private static final String COMMAND_NAME = "CONFIG";
    private static final String SUPPORTED_SUBCOMMAND = "GET";
    private static final int MIN_ARG_COUNT = 3;
    private static final int MAX_ARG_COUNT = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(final CommandContext context) {

        return CommandValidator.argRange(MIN_ARG_COUNT, MAX_ARG_COUNT).and(
                CommandValidator.argEquals(1, SUPPORTED_SUBCOMMAND)).validate(context);

    }

    @Override
    protected CommandResult executeInternal(final CommandContext context) {
        final String parameter = context.getArg(2);
        final var config = context.getServerContext().getConfig();

        return config.getConfigParameter(parameter)
                .map(value -> {
                    LOGGER.debug("CONFIG GET for parameter '{}': '{}'", parameter, value);
                    if ("*".equals(parameter)) {
                        final String[] parts = value.split(" ");
                        final List<String> configList = new ArrayList<>();
                        for (int i = 0; i < parts.length - 1; i += 2) {
                            if (i + 1 < parts.length) {
                                configList.add(parts[i]);
                                configList.add(parts[i + 1]);
                            }
                        }
                        return CommandResult.success(ResponseBuilder.array(configList));
                    } else {
                        return CommandResult.success(ResponseBuilder.array(List.of(parameter, value)));
                    }
                })
                .orElseGet(() -> {
                    LOGGER.info("Unknown configuration parameter requested: '{}'", parameter);
                    return CommandResult.error("Unknown configuration parameter: " + parameter);
                });
    }
}