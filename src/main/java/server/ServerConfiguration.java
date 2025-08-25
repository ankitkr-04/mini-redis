package server;

import java.util.Map;
import java.util.Optional;

import config.ConfigurationParser;
import config.ConfigurationParser.MasterInfo;

public record ServerConfiguration(
        int port,
        Optional<MasterInfo> masterInfo,
        long replicationBacklogSize,
        String dataDirectory,
        String databaseFilename,
        boolean appendOnlyMode,
        long maxMemory,
        String bindAddress,
        Optional<String> requirePassword) {

    // Default values
    private static final int DEFAULT_PORT = 6379;
    private static final long DEFAULT_REPL_BACKLOG_SIZE = 1024 * 1024; // 1MB
    private static final String DEFAULT_DIR = "/var/lib/redis";
    private static final String DEFAULT_DB_FILENAME = "dump.rdb";
    private static final long DEFAULT_MAX_MEMORY = 100 * 1024 * 1024; // 100MB
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    public static ServerConfiguration from(Map<String, String> options) {
        return new ServerConfiguration(
                ConfigurationParser.getIntOption(options, "port", DEFAULT_PORT),
                ConfigurationParser.getMasterInfoOption(options, "replicaof"),
                ConfigurationParser.getLongOption(options, "repl-backlog-size", DEFAULT_REPL_BACKLOG_SIZE),
                ConfigurationParser.getStringOption(options, "dir", DEFAULT_DIR),
                ConfigurationParser.getStringOption(options, "dbfilename", DEFAULT_DB_FILENAME),
                ConfigurationParser.getBooleanOption(options, "appendonly", false),
                ConfigurationParser.getLongOption(options, "maxmemory", DEFAULT_MAX_MEMORY),
                ConfigurationParser.getStringOption(options, "bind", DEFAULT_BIND_ADDRESS),
                Optional.ofNullable(options.get("requirepass")));
    }

    public boolean isReplicaMode() {
        return masterInfo.isPresent();
    }

    public MasterInfo getMasterInfo() {
        return masterInfo.orElseThrow(() -> new IllegalStateException("Not in replica mode"));
    }

    public Optional<String> getConfigParameter(String parameter) {
        return switch (parameter.toLowerCase()) {
            case "port" -> Optional.of(String.valueOf(port));
            case "repl-backlog-size" -> Optional.of(String.valueOf(replicationBacklogSize));
            case "dir" -> Optional.of(dataDirectory);
            case "dbfilename" -> Optional.of(databaseFilename);
            case "appendonly" -> Optional.of(appendOnlyMode ? "yes" : "no");
            case "maxmemory" -> Optional.of(String.valueOf(maxMemory));
            case "bind" -> Optional.of(bindAddress);
            case "requirepass" -> Optional.of(requirePassword.orElse(""));
            case "master_host" -> Optional.of(isReplicaMode() ? getMasterInfo().host() : "");
            case "master_port" -> Optional.of(isReplicaMode() ? String.valueOf(getMasterInfo().port()) : "0");
            default -> Optional.empty();
        };
    }
}