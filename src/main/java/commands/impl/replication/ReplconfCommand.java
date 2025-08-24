package commands.impl.replication;

import java.nio.ByteBuffer;
import java.util.Arrays;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import config.RedisConstants;
import core.ServerContext;
import protocol.ResponseBuilder;
import storage.StorageService;

public class ReplconfCommand implements Command {
    private final ServerContext context;

    public String name() {
        return "REPLCONF";
    }

    public ReplconfCommand(ServerContext context) {
        this.context = context;
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() >= 3 && args.argCount() % 2 == 1; // Command + pairs of key-value
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageService storage) {
        String[] rawArgs = args.rawArgs();
        
        // Process REPLCONF parameters in pairs (key, value)
        for (int i = 1; i < rawArgs.length; i += 2) {
            if (i + 1 >= rawArgs.length) {
                return new CommandResult.Error("Missing value for REPLCONF parameter: " + rawArgs[i]);
            }
            
            String key = rawArgs[i].toLowerCase();
            String value = rawArgs[i + 1];
            
            switch (key) {
                case "listening-port" -> {
                    // Slave is telling us its listening port
                    try {
                        int port = Integer.parseInt(value);
                        System.out.println("Replica listening on port: " + port);
                        // Store this information if needed
                    } catch (NumberFormatException e) {
                        return new CommandResult.Error("Invalid port number: " + value);
                    }
                }
                case "capa" -> {
                    // Replica is telling us its capabilities
                    System.out.println("Replica capability: " + value);
                    // Handle capabilities like "eof", "psync2", etc.
                }
                case "ack" -> {
                    // Replica is acknowledging replication offset
                    try {
                        long offset = Long.parseLong(value);
                        System.out.println("Replica ACK offset: " + offset);
                        // Update replica's acknowledged offset
                        return new CommandResult.Success(ResponseBuilder.integer(context.getServerInfo()
                            .getReplicationInfo().getMasterReplOffset()));
                    } catch (NumberFormatException e) {
                        return new CommandResult.Error("Invalid offset: " + value);
                    }
                }
                case "getack" -> {
                    // Master is requesting ACK from replica
                    if ("*".equals(value)) {
                        // Reply with current offset
                        long currentOffset = context.getServerInfo().getReplicationInfo().getMasterReplOffset();
                        ByteBuffer response = ResponseBuilder.array(Arrays.asList("REPLCONF", "ACK", String.valueOf(currentOffset)));
                        return new CommandResult.Success(response);
                    }
                }
                default -> {
                    System.out.println("Unknown REPLCONF parameter: " + key + " = " + value);
                    // For compatibility, we acknowledge unknown parameters
                }
            }
        }
        
        // Default response is OK for most REPLCONF commands
        return new CommandResult.Success(ResponseBuilder.encode(RedisConstants.OK_RESPONSE));
    }
}