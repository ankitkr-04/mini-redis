package commands;

import java.nio.ByteBuffer;
import java.util.Map;
import commands.list.LLenCommand;
import commands.list.LPushCommand;
import commands.list.LRangeCommand;
import commands.list.RPushCommand;
import commands.string.GetCommand;
import commands.string.SetCommand;
import commands.util.EchoCommand;
import commands.util.PingCommand;
import resp.RESPFormatter;
import store.DataStore;

public final class CommandHandler {

    private final Map<String, ICommand> commands;

    public CommandHandler() {
        commands = Map.of("PING", new PingCommand(), "ECHO", new EchoCommand(), "SET",
                new SetCommand(), "GET", new GetCommand(), "RPUSH", new RPushCommand(), "LRANGE",
                new LRangeCommand(), "LPUSH", new LPushCommand(), "LLEN", new LLenCommand());
    }

    public ByteBuffer handle(String[] args, DataStore dataStore) {
        if (args.length == 0) {
            return RESPFormatter.error("ERR unknown command");
        }

        ICommand command = commands.get(args[0].toUpperCase());
        if (command == null) {
            return RESPFormatter.error("ERR unknown command");
        }

        if (!command.validateArgs(args)) {
            return RESPFormatter.error("ERR wrong number of arguments");
        }

        return command.execute(args, dataStore);
    }
}
