package responses;

import java.nio.ByteBuffer;
import java.util.Collections;
import resp.RESPFormatter;

public final class StandardResponses {
    public static final ByteBuffer PONG = RESPFormatter.simpleString("PONG");
    public static final ByteBuffer OK = RESPFormatter.simpleString("OK");
    public static final ByteBuffer NULL = RESPFormatter.nullBulkString();
    public static final ByteBuffer UNKNOWN_COMMAND = RESPFormatter.error("ERR unknown command");
    public static final ByteBuffer WRONG_ARGS =
            RESPFormatter.error("ERR wrong number of arguments");
    public static final ByteBuffer EMPTY_ARRAY = RESPFormatter.array(Collections.emptyList());
}
