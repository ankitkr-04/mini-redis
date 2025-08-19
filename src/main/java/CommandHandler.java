import java.nio.ByteBuffer;
import java.util.Map;
import commands.EchoCommand;
import commands.GetCommand;
import commands.ICommand;
import commands.LRangeCommand;
import commands.PingCommand;
import commands.RPushCommand;
import commands.SetCommand;
import resp.RESPFormatter;
import store.DataStore;

public final class CommandHandler {

    private final Map<String, ICommand> commands;

    public CommandHandler() {
        commands = Map.of("PING", new PingCommand(), "ECHO", new EchoCommand(), "SET",
                new SetCommand(), "GET", new GetCommand(), "RPUSH", new RPushCommand(), "LRANGE",
                new LRangeCommand());
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
