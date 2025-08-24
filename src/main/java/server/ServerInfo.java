package server;

import java.nio.ByteBuffer;
import java.util.Map;
import config.RedisConstants;
import protocol.ResponseBuilder;
import server.info.ReplicationInfo;

public class ServerInfo {
    private static final String REPLICATION_SECTION = "replication";
    private static final String SECTION_HEADER_PREFIX = "# ";

    private final ReplicationInfo replicationInfo;
    private final ServerOptions options;

    public ServerInfo(ServerOptions options) {
        this.options = options;
        this.replicationInfo = new ReplicationInfo(options.masterInfo(), options.replBacklogSize());
    }

    public ReplicationInfo getReplicationInfo() {
        return replicationInfo;
    }

    public ByteBuffer getInfoResponse(String section) {
        String normalizedSection = section == null ? "" : section.toLowerCase();
        StringBuilder response = new StringBuilder();

        // If no section specified or "replication", include replication section
        if (normalizedSection.isEmpty() || normalizedSection.equals(REPLICATION_SECTION)) {
            response.append(SECTION_HEADER_PREFIX).append("Replication")
                    .append(RedisConstants.CRLF);
            response.append(formatSection(replicationInfo.toInfoMap()));
        }

        // Add more sections here in the future (e.g., server, memory, persistence)

        return ResponseBuilder.bulkString(response.toString());
    }

    private String formatSection(Map<String, String> info) {
        StringBuilder section = new StringBuilder();
        info.forEach((key, value) -> {
            if (value != null) {
                section.append(key).append(":").append(value).append(RedisConstants.CRLF);
            }
        });
        return section.toString();
    }
}
