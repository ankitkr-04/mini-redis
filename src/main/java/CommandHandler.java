import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record ValueWithExpiry(String value, long expiry) {
    private static final long NO_EXPIRY = -1L;

    public static ValueWithExpiry noExpiry(String val) {
        return new ValueWithExpiry(val, NO_EXPIRY);
    }
}


public class CommandHandler {

    private static final String PING_CMD = "PING";
    private static final String ECHO_CMD = "ECHO";
    private static final String SET_CMD = "SET";
    private static final String GET_CMD = "GET";


    private static final ByteBuffer PONG_RESPONSE =
            ByteBuffer.wrap("+PONG\r\n".getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
    private static final ByteBuffer OK_RESPONSE =
            ByteBuffer.wrap("+OK\r\n".getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
    private static final ByteBuffer NULL_RESPONSE =
            ByteBuffer.wrap("$-1\r\n".getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
    private static final ByteBuffer UNKNOWN_CMD_ERROR = ByteBuffer
            .wrap("-ERR unknown command\r\n".getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
    private static final ByteBuffer WRONG_ARGS_ERROR =
            ByteBuffer.wrap("-ERR wrong number of arguments\r\n".getBytes(StandardCharsets.UTF_8))
                    .asReadOnlyBuffer();

    private final Map<String, ValueWithExpiry> store = new ConcurrentHashMap<>();

    // Helper method for bulk strings
    private ByteBuffer bulkString(String msg) {
        if (msg == null)
            return NULL_RESPONSE.duplicate();
        return ByteBuffer.wrap(
                ("$" + msg.length() + "\r\n" + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    public ByteBuffer handle(String[] commands) {
        if (commands.length == 0)
            return UNKNOWN_CMD_ERROR.duplicate();

        switch (commands[0].toUpperCase()) {
            case PING_CMD:
                return PONG_RESPONSE.duplicate();

            case ECHO_CMD:
                if (commands.length != 2)
                    return WRONG_ARGS_ERROR.duplicate();
                return bulkString(commands[1]);

            case SET_CMD:
                int n = commands.length;
                if (n == 3) {
                    store.put(commands[1], ValueWithExpiry.noExpiry(commands[2]));
                } else if (n == 5 && commands[3].equalsIgnoreCase("PX")) {
                    long expiryTime = System.currentTimeMillis() + Long.parseLong(commands[4]);
                    store.put(commands[1], new ValueWithExpiry(commands[2], expiryTime));
                } else {
                    return WRONG_ARGS_ERROR.duplicate();
                }
                return OK_RESPONSE.duplicate();

            case GET_CMD:
                if (commands.length != 2)
                    return WRONG_ARGS_ERROR.duplicate();

                ValueWithExpiry record = store.get(commands[1]);
                if (record == null)
                    return NULL_RESPONSE.duplicate();

                long now = System.currentTimeMillis();
                if (record.expiry() != -1 && now >= record.expiry()) {
                    store.remove(commands[1]); // remove expired key
                    return NULL_RESPONSE.duplicate();
                }

                return bulkString(record.value());

            default:
                return UNKNOWN_CMD_ERROR.duplicate();
        }
    }
}
