package commands.impl.replication;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.CommandArgs;
import commands.CommandResult;
import core.ServerContext;
import protocol.ResponseBuilder;
import validation.ValidationResult;
import validation.ValidationUtils;

@FunctionalInterface
public interface ReplconfHandler {
    CommandResult handle(String key, String value, CommandArgs args, ServerContext context);

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
    public CommandResult handle(String key, String value, CommandArgs args, ServerContext context) {
        ValidationResult validation = ValidationUtils.validateInteger(value);
        if (!validation.isValid()) {
            log.debug("Invalid port number: {}", value);
            return CommandResult.error("Invalid port number: " + value);
        }

        log.debug("Replica listening on port: {}", value);
        return null; // Success, continue processing
    }
}

class CapabilityHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(CapabilityHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandArgs args, ServerContext context) {
        log.debug("Replica capability: {}", value);
        return null; // Success, continue processing
    }
}

class AckHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(AckHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandArgs args, ServerContext context) {
        ValidationResult validation = ValidationUtils.validateInteger(value);
        if (!validation.isValid()) {
            log.debug("Invalid offset: {}", value);
            return CommandResult.error("Invalid offset: " + value);
        }

        long offset = Long.parseLong(value);
        log.trace("Replica ACK offset: {}", offset);

        context.getReplicationManager().updateReplicaOffset(args.clientChannel(), offset);
        long currentOffset = context.getServerInfo().getReplicationInfo().getMasterReplOffset();

        return CommandResult.success(ResponseBuilder.integer(currentOffset));
    }
}

class GetAckHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(GetAckHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandArgs args, ServerContext context) {
        if ("*".equals(value)) {
            long currentOffset = context.getServerInfo().getReplicationInfo().getMasterReplOffset();
            ByteBuffer response = ResponseBuilder.array(
                    List.of("REPLCONF", "ACK", String.valueOf(currentOffset)));
            return CommandResult.success(response);
        }

        log.debug("Unknown getack value: {}", value);
        return null; // Continue processing
    }
}

class UnknownHandler implements ReplconfHandler {
    private static final Logger log = LoggerFactory.getLogger(UnknownHandler.class);

    @Override
    public CommandResult handle(String key, String value, CommandArgs args, ServerContext context) {
        log.debug("Unknown REPLCONF parameter: {} = {}", key, value);
        return null; // Continue processing
    }
}