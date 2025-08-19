import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record ValueWithExpiry(String value, long expiry) {
    private static final long NO_EXPIRY = -1l;

    public static ValueWithExpiry noExpiry(String val) {
        return new ValueWithExpiry(val, NO_EXPIRY);
    }
}


public class CommandHandler {
    private final Map<String, ValueWithExpiry> store = new ConcurrentHashMap<>();

    // Helper methods
    private ByteBuffer simpleString(String msg) {
        return ByteBuffer.wrap(("+" + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private ByteBuffer bulkString(String msg) {
        if (msg == null)
            return ByteBuffer.wrap("$-1\r\n".getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(
                ("$" + msg.length() + "\r\n" + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private ByteBuffer error(String msg) {
        return ByteBuffer.wrap(("-ERR " + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    public ByteBuffer handle(String[] commands) {
        if (commands.length == 0)
            return error("unknown command");

        switch (commands[0].toUpperCase()) {
            case "PING":
                return simpleString("PONG");

            case "ECHO":
                if (commands.length != 2)
                    return error("wrong number of arguments for 'echo' command");
                return bulkString(commands[1]);

            case "SET":

                int n = commands.length;

                if (n == 3) {
                    store.put(commands[1], ValueWithExpiry.noExpiry(commands[2]));
                } else if (n == 5 && commands[3].equalsIgnoreCase("PX")) {
                    long expiryTime = Long.parseLong(commands[4]) + System.currentTimeMillis();
                    store.put(commands[1], new ValueWithExpiry(commands[2], expiryTime));
                } else {
                    return error("wrong number of arguments for 'set' command");
                }
                return simpleString("OK");

            case "GET":
                var record = store.get(commands[1]);
                if (record == null)
                    return bulkString(null);
                var now = System.currentTimeMillis();
                if (record.expiry() != -1 && now >= record.expiry()) {
                    store.remove(commands[1]); // remove expired key
                    return bulkString(null); // return null bulk string
                }

                return bulkString(record.value());

            default:
                return error("unknown command");
        }
    }
}
