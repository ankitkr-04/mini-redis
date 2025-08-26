package commands.core;

/**
 * Represents the types of commands supported by the system.
 * <p>
 * Each command type indicates the nature of the operation:
 * <ul>
 * <li>{@link #READ} - Operations that only read data.</li>
 * <li>{@link #WRITE} - Operations that modify data.</li>
 * <li>{@link #REPLICATION} - Operations related to data replication.</li>
 * <li>{@link #TRANSACTION} - Operations that are part of a transaction.</li>
 * <li>{@link #BLOCKING} - Operations that may block execution.</li>
 * </ul>
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public enum CommandType {
    READ,
    WRITE,
    REPLICATION,
    TRANSACTION,
    BLOCKING;

    /**
     * The total number of command types.
     */
    public static final int COMMAND_TYPE_COUNT = values().length;
}
