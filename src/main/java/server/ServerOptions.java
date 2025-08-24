package server;

import java.util.Map;
import java.util.Optional;

import config.ServerConfig;
import protocol.parser.CommandLineParser;

public record ServerOptions(
                int port,
                Optional<CommandLineParser.MasterInfo> masterInfo,
                long replBacklogSize,
                String dir,
                String dbFilename,
                boolean appendOnly,
                long maxMemory,
                String bindAddress,
                Optional<String> requirePass) {

        public static ServerOptions from(Map<String, String> options) {
                return new ServerOptions(
                                CommandLineParser.getIntOption(options, "port",
                                                ServerConfig.DEFAULT_PORT),
                                CommandLineParser.getMasterInfoOption(options, "replicaof"),
                                CommandLineParser.getLongOption(options, "repl-backlog-size",
                                                ServerConfig.DEFAULT_REPL_BACKLOG_SIZE),
                                CommandLineParser.getStringOption(options, "dir",
                                                ServerConfig.DEFAULT_DIR),
                                CommandLineParser.getStringOption(options, "dbfilename",
                                                ServerConfig.DEFAULT_DB_FILENAME),
                                CommandLineParser.getBooleanOption(options, "appendonly", false),
                                CommandLineParser.getLongOption(options, "maxmemory",
                                                ServerConfig.DEFAULT_MAX_MEMORY),
                                CommandLineParser.getStringOption(options, "bind",
                                                ServerConfig.DEFAULT_BIND_ADDRESS),
                                Optional.ofNullable(options.get("requirepass")));
        }
}
