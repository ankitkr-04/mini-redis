package commands.context;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import server.ServerContext;
import storage.StorageService;

public final class CommandContext {
    private final String operation;
    private final String[] args;
    private final SocketChannel clientChannel;
    private final StorageService storageService;
    private final ServerContext serverContext;

    public CommandContext(String operation, String[] args, SocketChannel clientChannel,
            StorageService storageService, ServerContext serverContext) {
        this.operation = operation.toUpperCase();
        this.args = args;
        this.clientChannel = clientChannel;
        this.storageService = storageService;
        this.serverContext = serverContext;
    }

    // Getters
    public String getOperation() {
        return operation;
    }

    public String[] getArgs() {
        return args;
    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public ServerContext getServerContext() {
        return serverContext;
    }

    // Argument access methods
    public String getArg(int index) {
        return index < args.length ? args[index] : null;
    }

    public String getKey() {
        return hasKey() ? args[1] : null;
    }

    public String getValue() {
        return hasValue() ? args[2] : null;
    }

    public String[] getValues() {
        return args.length <= 2 ? new String[0] : Arrays.copyOfRange(args, 2, args.length);
    }

    public List<String> getSlice(int start, int end) {
        if (start < 0 || end > args.length || start > end) {
            throw new IndexOutOfBoundsException("Invalid slice range: " + start + " to " + end);
        }
        return Arrays.asList(args).subList(start, end);
    }

    // Validation helpers
    public boolean hasKey() {
        return args.length >= 2;
    }

    public boolean hasValue() {
        return args.length >= 3;
    }

    public int getArgCount() {
        return args.length;
    }

    // Parsing utilities
    public int getIntArg(int index) {
        return Integer.parseInt(getArg(index));
    }

    public long getLongArg(int index) {
        return Long.parseLong(getArg(index));
    }

    public double getDoubleArg(int index) {
        return Double.parseDouble(getArg(index));
    }

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