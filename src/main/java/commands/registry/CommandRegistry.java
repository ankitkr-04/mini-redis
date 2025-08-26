package commands.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.core.Command;

/**
 * Maintains a registry of command implementations for the Redis protocol.
 * Allows registration and lookup of commands by name or alias.
 */
public final class CommandRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);

    /** Thread-safe map of command names/aliases to Command instances. */
    private final Map<String, Command> commandMap = new ConcurrentHashMap<>();

    /**
     * Registers a command with its primary name.
     * 
     * @param command the command to register
     */
    public void register(Command command) {
        String commandName = normalizeName(command.getName());
        commandMap.put(commandName, command);
        LOGGER.debug("Registered command: {}", commandName);
    }

    /**
     * Registers a command with its primary name and additional aliases.
     * 
     * @param command the command to register
     * @param aliases alternative names for the command
     */
    public void register(Command command, String... aliases) {
        register(command);
        for (String alias : aliases) {
            String normalizedAlias = normalizeName(alias);
            commandMap.put(normalizedAlias, command);
            LOGGER.debug("Registered alias '{}' for command '{}'", normalizedAlias, command.getName());
        }
    }

    /**
     * Retrieves a command by name or alias.
     * 
     * @param name the name or alias of the command
     * @return the Command instance, or null if not found
     */
    public Command getCommand(String name) {
        return commandMap.get(normalizeName(name));
    }

    /**
     * Checks if a command with the given name or alias exists.
     * 
     * @param name the name or alias to check
     * @return true if the command exists, false otherwise
     */
    public boolean hasCommand(String name) {
        return commandMap.containsKey(normalizeName(name));
    }

    /**
     * Returns the number of registered commands (including aliases).
     * 
     * @return the size of the registry
     */
    public int size() {
        return commandMap.size();
    }

    /**
     * Normalizes command names to uppercase for consistent lookup.
     * 
     * @param name the command name or alias
     * @return the normalized name
     */
    private String normalizeName(String name) {
        return name == null ? null : name.toUpperCase();
    }
}
