package commands.context;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import server.ServerContext;
import storage.StorageService;

/**
 * Context for a command execution, encapsulating operation, arguments, client
 * channel,
 * storage service, and server context.
 */
public final class CommandContext {


    // Argument index constants
    private static final int KEY_INDEX = 1;
    private static final int VALUE_INDEX = 2;

    private final String operation;
    private final String[] args;
    private final SocketChannel clientChannel;
    private final StorageService storageService;
    private final ServerContext serverContext;

    /**
     * Constructs a CommandContext with the given parameters.
     *
     * @param operation      the command operation (e.g., "SET", "GET")
     * @param args           the command arguments
     * @param clientChannel  the client's socket channel
     * @param storageService the storage service instance
     * @param serverContext  the server context
     */
    public CommandContext(String operation, String[] args, SocketChannel clientChannel,
            StorageService storageService, ServerContext serverContext) {
        this.operation = operation.toUpperCase();
        this.args = args;
        this.clientChannel = clientChannel;
        this.storageService = storageService;
        this.serverContext = serverContext;
    }

    /**
     * Returns the command operation.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns the command arguments.
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Returns the client socket channel.
     */
    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    /**
     * Returns the storage service instance.
     */
    public StorageService getStorageService() {
        return storageService;
    }

    /**
     * Returns the server context.
     */
    public ServerContext getServerContext() {
        return serverContext;
    }

    /**
     * Returns the argument at the specified index, or null if out of bounds.
     */
    public String getArg(int index) {
        return index < args.length ? args[index] : null;
    }

    /**
     * Returns the key argument if present, otherwise null.
     */
    public String getKey() {
        return hasKey() ? args[KEY_INDEX] : null;
    }

    /**
     * Returns the value argument if present, otherwise null.
     */
    public String getValue() {
        return hasValue() ? args[VALUE_INDEX] : null;
    }

    /**
     * Returns all arguments after the value argument as an array.
     */
    public String[] getValues() {
        return args.length <= VALUE_INDEX ? new String[0] : Arrays.copyOfRange(args, VALUE_INDEX, args.length);
    }

    /**
     * Returns a sublist of arguments from start (inclusive) to end (exclusive).
     *
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public List<String> getSlice(int start, int end) {
        if (start < 0 || end > args.length || start > end) {
            throw new IndexOutOfBoundsException("Invalid slice range: " + start + " to " + end);
        }
        return Arrays.asList(args).subList(start, end);
    }

    /**
     * Returns true if a key argument is present.
     */
    public boolean hasKey() {
        return args.length > KEY_INDEX;
    }

    /**
     * Returns true if a value argument is present.
     */
    public boolean hasValue() {
        return args.length > VALUE_INDEX;
    }

    /**
     * Returns the number of arguments.
     */
    public int getArgCount() {
        return args.length;
    }

    /**
     * Parses the argument at the given index as an int.
     *
     * @throws NumberFormatException if the argument cannot be parsed
     */
    public int getIntArg(int index) {
        return Integer.parseInt(getArg(index));
    }

    /**
     * Parses the argument at the given index as a long.
     *
     * @throws NumberFormatException if the argument cannot be parsed
     */
    public long getLongArg(int index) {
        return Long.parseLong(getArg(index));
    }

    /**
     * Parses the argument at the given index as a double.
     *
     * @throws NumberFormatException if the argument cannot be parsed
     */
    public double getDoubleArg(int index) {
        return Double.parseDouble(getArg(index));
    }

    /**
     * Returns a map of field-value pairs starting from the given index.
     *
     * @param startIndex the index to start parsing field-value pairs
     * @return a map of field-value pairs
     * @throws IllegalArgumentException if the number of remaining arguments is odd
     */
    public Map<String, String> getFieldValueMap(int startIndex) {
        Map<String, String> map = new HashMap<>();
        if (startIndex >= args.length) {
            return map;
        }
        int remaining = args.length - startIndex;
        if (remaining % 2 != 0) {
            throw new IllegalArgumentException("Field-value pairs must be complete");
        }
        for (int i = startIndex; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }
}