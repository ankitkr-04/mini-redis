package commands.streams;

import java.util.List;
import java.util.Optional;
import commands.CommandArgs;

public record XReadArgs(
        Optional<Integer> count,
        Optional<Long> blockMs,
        List<String> keys,
        List<String> ids) {
    public static XReadArgs parse(CommandArgs args) {
        int count = -1;
        long blockMs = -1;
        int i = 1;

        while (i < args.argCount() && !args.arg(i).equalsIgnoreCase("STREAMS")) {
            switch (args.arg(i).toUpperCase()) {
                case "COUNT" -> {
                    if (i + 1 >= args.argCount())
                        throw new IllegalArgumentException("COUNT requires a value");
                    count = Integer.parseInt(args.arg(i + 1));
                    i += 2;
                }
                case "BLOCK" -> {
                    if (i + 1 >= args.argCount())
                        throw new IllegalArgumentException("BLOCK requires a value");
                    blockMs = Long.parseLong(args.arg(i + 1));
                    i += 2;
                }
                default -> throw new IllegalArgumentException(
                        String.format("Unexpected token: %s", args.arg(i)));
            }
        }

        if (i >= args.argCount() || !args.arg(i).equalsIgnoreCase("STREAMS")) {
            throw new IllegalArgumentException("Missing STREAMS keyword");
        }
        i++;

        int remaining = args.argCount() - i;
        if (remaining % 2 != 0) {
            throw new IllegalArgumentException("Wrong number of arguments for XREAD STREAMS");
        }

        int half = remaining / 2;
        List<String> keys = args.slice(i, i + half);
        List<String> ids = args.slice(i + half, args.argCount());

        return new XReadArgs(
                count > 0 ? Optional.of(count) : Optional.empty(),
                blockMs >= 0 ? Optional.of(blockMs) : Optional.empty(),
                keys,
                ids);
    }
}
