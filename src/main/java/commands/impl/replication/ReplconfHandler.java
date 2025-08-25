package commands.impl.replication;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

@FunctionalInterface
public interface ReplconfHandler {
    CommandResult handle(String key, String value, CommandContext context);

    static ReplconfHandler listeningPort() {
        return new ListeningPortHandler();
    }

    static ReplconfHandler capability() {
        return new CapabilityHandler();
    }

    static ReplconfHandler ack() {
        return new AckHandler();
    }

    static ReplconfHandler getAck() {
        return new GetAckHandler();
    }

    static ReplconfHandler unknown() {
        return new UnknownHandler();
    }
}

class ListeningPortHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(ListeningPortHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandContext context) {
        ValidationResult validation = CommandValidator.validateInteger(value);
        if (!validation.isValid()) {
            log.debug("Invalid port number: {}", value);
            return CommandResult.error("Invalid port number: " + value);
        }

        log.debug("Replica listening on port: {}", value);
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK)); // Return OK response
    }
}

class CapabilityHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(CapabilityHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandContext context) {
        log.debug("Replica capability: {}", value);
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK)); // Return OK response
    }
}

class AckHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(AckHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandContext context) {
        ValidationResult validation = CommandValidator.validateInteger(value);
        if (!validation.isValid()) {
            log.debug("Invalid offset: {}", value);
            return CommandResult.error("Invalid offset: " + value);
        }

        long offset = Long.parseLong(value);
        log.info("Replica ACK offset: {} from channel: {}", offset, context.getClientChannel());

        // Update the replica offset for this client
        context.getServerContext().getReplicationManager().updateReplicaOffset(context.getClientChannel(), offset);

        context.getServerContext().getReplicationManager().checkPendingWaits();

        // ACK commands should not send any response back to the replica
        return CommandResult.async();

    }
}

class GetAckHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(GetAckHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandContext context) {
        if ("*".equals(value)) {
            long currentOffset = context.getServerContext().getReplicationState().getMasterReplicationOffset();
            ByteBuffer response = ResponseBuilder.array(
                    List.of("REPLCONF", "ACK", String.valueOf(currentOffset)));
            return CommandResult.success(response);
        }

        log.debug("Unknown getack value: {}", value);
        return CommandResult.async(); // Continue
    }
}

class UnknownHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(UnknownHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandContext context) {
        log.debug("Unknown REPLCONF parameter: {} = {}", key, value);
        return CommandResult.async(); // Continue
    }
}