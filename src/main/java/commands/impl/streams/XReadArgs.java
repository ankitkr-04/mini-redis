package commands.impl.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import commands.context.CommandContext;
import storage.StorageService;

public record XReadArgs(
        Optional<Integer> count,
        Optional<Long> blockMs,
        List<String> keys,
        List<String> ids) {
    public static XReadArgs parse(CommandContext context) {
        int count = -1;
        long blockMs = -1;
        int i = 1;

        while (i < context.getArgCount() && !context.getArg(i).equalsIgnoreCase("STREAMS")) {
            switch (context.getArg(i).toUpperCase()) {
                case "COUNT" -> {
                    if (i + 1 >= context.getArgCount())
                        throw new IllegalArgumentException("COUNT requires a value");
                    count = Integer.parseInt(context.getArg(i + 1));
                    i += 2;
                }
                case "BLOCK" -> {
                    if (i + 1 >= context.getArgCount())
                        throw new IllegalArgumentException("BLOCK requires a value");
                    blockMs = Long.parseLong(context.getArg(i + 1));
                    i += 2;
                }
                default -> throw new IllegalArgumentException(
                        String.format("Unexpected token: %s", context.getArg(i)));
            }
        }

        if (i >= context.getArgCount() || !context.getArg(i).equalsIgnoreCase("STREAMS")) {
            throw new IllegalArgumentException("Missing STREAMS keyword");
        }
        i++;

        int remaining = context.getArgCount() - i;
        if (remaining % 2 != 0) {
            throw new IllegalArgumentException("Wrong number of arguments for XREAD STREAMS");
        }

        int half = remaining / 2;
        List<String> keys = context.getSlice(i, i + half);
        List<String> ids = context.getSlice(i + half, context.getArgCount());

        return new XReadArgs(
                count > 0 ? Optional.of(count) : Optional.empty(),
                blockMs >= 0 ? Optional.of(blockMs) : Optional.empty(),
                keys,
                ids);
    }

    public XReadArgs withResolvedIds(StorageService storage) {
        List<String> resolved = new ArrayList<>(ids.size());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String id = ids.get(i);
            if ("$".equals(id)) {
                Optional<String> last = storage.getLastStreamId(key);
                resolved.add(last.orElse("0-0"));
            } else {
                resolved.add(id);
            }
        }
        return new XReadArgs(count, blockMs, keys, List.copyOf(resolved));
    }
}