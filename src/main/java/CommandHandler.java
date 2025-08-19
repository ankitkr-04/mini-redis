import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandHandler {
    private final Map<String, String> store = new ConcurrentHashMap<>();

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
                if (commands.length != 3)
                    return error("wrong number of arguments for 'set' command");
                store.put(commands[1], commands[2]);
                return simpleString("OK");

            case "GET":
                if (commands.length != 2)
                    return error("wrong number of arguments for 'get' command");
                return bulkString(store.get(commands[1]));

            default:
                return error("unknown command");
        }
    }
}
