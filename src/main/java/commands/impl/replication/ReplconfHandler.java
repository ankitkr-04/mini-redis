/**
 * Handles different types of REPLCONF subcommands for Redis replication.
 * Provides factory methods for each supported REPLCONF handler.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
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
    /**
     * Handles a REPLCONF subcommand.
     *
     * @param parameterKey   the REPLCONF parameter key (e.g., "listening-port")
     * @param parameterValue the REPLCONF parameter value
     * @param context        the command context
     * @return the command result
     */
    CommandResult handle(String parameterKey, String parameterValue, CommandContext context);

    /**
     * Factory for listening-port handler.
     */
    static ReplconfHandler listeningPort() {
        return new ListeningPortHandler();
    }

    /**
     * Factory for capability handler.
     */
    static ReplconfHandler capability() {
        return new CapabilityHandler();
    }

    /**
     * Factory for ack handler.
     */
    static ReplconfHandler ack() {
        return new AckHandler();
    }

    /**
     * Factory for getack handler.
     */
    static ReplconfHandler getAck() {
        return new GetAckHandler();
    }

    /**
     * Factory for unknown handler.
     */
    static ReplconfHandler unknown() {
        return new UnknownHandler();
    }
}

/**
 * Handles the "listening-port" REPLCONF subcommand.
 * Validates and acknowledges the replica's listening port.
 */
class ListeningPortHandler implements ReplconfHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListeningPortHandler.class);
    private static final String INVALID_PORT_MESSAGE = "Invalid port number: ";

    @Override
    public CommandResult handle(String parameterKey, String parameterValue, CommandContext context) {
        ValidationResult portValidation = CommandValidator.validateInteger(parameterValue);
        if (!portValidation.isValid()) {
            LOGGER.info("{}{}", INVALID_PORT_MESSAGE, parameterValue);
            return CommandResult.error(INVALID_PORT_MESSAGE + parameterValue);
        }
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}

/**
 * Handles the "capability" REPLCONF subcommand.
 * Acknowledges the replica's capability.
 */
class CapabilityHandler implements ReplconfHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityHandler.class);

    @Override
    public CommandResult handle(String parameterKey, String parameterValue, CommandContext context) {
        LOGGER.debug("Replica capability received: {}", parameterValue);
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}

/**
 * Handles the "ack" REPLCONF subcommand.
 * Updates the replica's replication offset.
 */
class AckHandler implements ReplconfHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AckHandler.class);
    private static final String INVALID_OFFSET_MESSAGE = "Invalid offset: ";

    @Override
    public CommandResult handle(String parameterKey, String parameterValue, CommandContext context) {
        ValidationResult offsetValidation = CommandValidator.validateInteger(parameterValue);
        if (!offsetValidation.isValid()) {
            LOGGER.info("{}{}", INVALID_OFFSET_MESSAGE, parameterValue);
            return CommandResult.error(INVALID_OFFSET_MESSAGE + parameterValue);
        }

        long replicaOffset = Long.parseLong(parameterValue);
        context.getServerContext().getReplicationManager()
                .updateReplicaOffset(context.getClientChannel(), replicaOffset);
        context.getServerContext().getReplicationManager().checkPendingWaits();

        // No response sent for ACK commands.
        return CommandResult.async();
    }
}

/**
 * Handles the "getack" REPLCONF subcommand.
 * Responds with the master's current replication offset.
 */
class GetAckHandler implements ReplconfHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetAckHandler.class);
    private static final String GETACK_WILDCARD = "*";

    @Override
    public CommandResult handle(String parameterKey, String parameterValue, CommandContext context) {
        if (GETACK_WILDCARD.equals(parameterValue)) {
            long masterOffset = context.getServerContext().getReplicationState().getMasterReplicationOffset();
            ByteBuffer response = ResponseBuilder.array(
                    List.of("REPLCONF", "ACK", String.valueOf(masterOffset)));
            return CommandResult.success(response);
        }
        LOGGER.debug("Unknown getack value: {}", parameterValue);
        return CommandResult.async();
    }
}

/**
 * Handles unknown REPLCONF subcommands.
 */
class UnknownHandler implements ReplconfHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnknownHandler.class);

    @Override
    public CommandResult handle(String parameterKey, String parameterValue, CommandContext context) {
        LOGGER.debug("Unknown REPLCONF parameter: {} = {}", parameterKey, parameterValue);
        return CommandResult.async();
    }
}