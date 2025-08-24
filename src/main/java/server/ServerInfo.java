package server;

import java.nio.ByteBuffer;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;
import protocol.ResponseBuilder;
import server.replication.ReplicationInfo;

public class ServerInfo {
    private static final Logger log = LoggerFactory.getLogger(ServerInfo.class);
    private static final String REPLICATION_SECTION = "replication";
    private static final String SECTION_HEADER_PREFIX = "# ";

    private final ReplicationInfo replicationInfo;
    private final ServerOptions options;

    public ServerInfo(ServerOptions options) {
        this.options = options;
        this.replicationInfo = new ReplicationInfo(options.masterInfo(), options.replBacklogSize());
        log.info("Server info initialized with options: {}", options);
    }

    public ReplicationInfo getReplicationInfo() {
        return replicationInfo;
    }

    public ServerOptions getOptions() {
        return options;
    }

    public ByteBuffer getInfoResponse(String section) {
        String normalizedSection = section == null ? "" : section.toLowerCase();
        StringBuilder response = new StringBuilder();

        if (normalizedSection.isEmpty() || normalizedSection.equals(REPLICATION_SECTION)) {
            response.append(SECTION_HEADER_PREFIX).append("Replication")
                    .append(ProtocolConstants.CRLF);
            response.append(formatSection(replicationInfo.toInfoMap()));
        }

        return ResponseBuilder.bulkString(response.toString());
    }

    private String formatSection(Map<String, String> info) {
        StringBuilder section = new StringBuilder();
        info.forEach((key, value) -> {
            if (value != null) {
                section.append(key).append(":").append(value).append(ProtocolConstants.CRLF);
            }
        });
        return section.toString();
    }
}