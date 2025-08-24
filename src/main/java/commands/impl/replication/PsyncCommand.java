package commands.impl.replication;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import core.ServerContext;
import protocol.ResponseBuilder;
import server.replication.ReplicationInfo;
import storage.StorageService;

public class PsyncCommand implements Command {
    private final ServerContext context;

    public PsyncCommand(ServerContext context) {
        this.context = context;
    }

    public String name() {
        return "PSYNC";
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 3; // PSYNC <replication_id> <offset>
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageService storage) {
        String[] rawArgs = args.rawArgs();
        String replId = rawArgs[1];
        String offsetStr = rawArgs[2];

        ReplicationInfo replInfo = context.getServerInfo().getReplicationInfo();

        // Parse offset
        long requestedOffset;
        try {
            requestedOffset = "?".equals(offsetStr) ? -1 : Long.parseLong(offsetStr);
        } catch (NumberFormatException e) {
            return new CommandResult.Error("Invalid offset: " + offsetStr);
        }

        // Check if this is a full resync request or partial resync
        boolean isFullResync = "?".equals(replId) || !replId.equals(replInfo.getMasterReplId())
                || requestedOffset == -1
                || requestedOffset < replInfo.getReplBacklogFirstByteOffset();

        if (isFullResync) {
            // Perform full resync
            return handleFullResync(args.clientChannel(), replInfo);
        } else {
            // Perform partial resync (not implemented in this basic version)
            return handlePartialResync(requestedOffset, replInfo);
        }
    }

    private CommandResult handleFullResync(SocketChannel clientChannel, ReplicationInfo replInfo) {
        try {
            // Send FULLRESYNC response
            String response = "+FULLRESYNC " + replInfo.getMasterReplId() + " " +
                    replInfo.getMasterReplOffset() + "\r\n";
            ByteBuffer responseBuffer = ResponseBuilder.encode(response);

            // Send the response
            while (responseBuffer.hasRemaining()) {
                clientChannel.write(responseBuffer);
            }

            // Send empty RDB file (for basic implementation)
            sendEmptyRdbFile(clientChannel);

            // Register this client as a replica
            replInfo.incrementSlaves();
            System.out.println("Full resync initiated for replica. Connected replicas: " +
                    replInfo.getConnectedSlaves());

            return new CommandResult.Async(); // No additional response needed

        } catch (Exception e) {
            System.err.println("Error during full resync: " + e.getMessage());
            return new CommandResult.Error("Full resync failed");
        }
    }

    private CommandResult handlePartialResync(long offset, ReplicationInfo replInfo) {
        // For basic implementation, we don't support partial resync
        // In a full implementation, you would:
        // 1. Check if the offset is within the replication backlog
        // 2. Send CONTINUE response
        // 3. Send the missing commands from the backlog

        // For now, fall back to full resync
        return new CommandResult.Error("Partial resync not supported, requesting full resync");
    }

    private void sendEmptyRdbFile(SocketChannel clientChannel) throws Exception {
        // Send an empty RDB file
        // RDB file format: REDIS<version><databases><EOF><checksum>
        // For simplicity, we send a minimal empty RDB file

        String emptyRdb = "REDIS0011\u00fe\u0000\u00ff"; // Basic empty RDB structure
        byte[] rdbBytes = emptyRdb.getBytes("ISO-8859-1");

        // Send RDB file size as bulk string format
        String sizeHeader = "$" + rdbBytes.length + "\r\n";
        ByteBuffer sizeBuffer = ResponseBuilder.encode(sizeHeader);

        while (sizeBuffer.hasRemaining()) {
            clientChannel.write(sizeBuffer);
        }

        // Send RDB content
        ByteBuffer rdbBuffer = ByteBuffer.wrap(rdbBytes);
        while (rdbBuffer.hasRemaining()) {
            clientChannel.write(rdbBuffer);
        }

        System.out.println("Sent empty RDB file to replica (" + rdbBytes.length + " bytes)");
    }
}
