package command;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import command.list.BLPopCommand;
import command.list.LLenCommand;
import command.list.LPopCommand;
import command.list.LPushCommand;
import command.list.LRangeCommand;
import command.list.RPushCommand;
import command.string.GetCommand;
import command.string.SetCommand;
import command.util.EchoCommand;
import command.util.PingCommand;
import command.util.TypeCommand;
import resp.RESPFormatter;
import store.DataStore;

public final class CommandHandler {

    private final Map<String, ICommand> commands;

    public CommandHandler() {
        commands = Map.ofEntries(Map.entry("PING", new PingCommand()),
                Map.entry("ECHO", new EchoCommand()), Map.entry("SET", new SetCommand()),
                Map.entry("GET", new GetCommand()), Map.entry("RPUSH", new RPushCommand()),
                Map.entry("LRANGE", new LRangeCommand()), Map.entry("LPUSH", new LPushCommand()),
                Map.entry("LLEN", new LLenCommand()), Map.entry("LPOP", new LPopCommand()),
                Map.entry("BLPOP", new BLPopCommand()), Map.entry("TYPE", new TypeCommand()));
    }

    public ByteBuffer handle(String[] args, DataStore dataStore, SocketChannel clientChannel) {
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

        return command.execute(args, dataStore, clientChannel);
    }
}
