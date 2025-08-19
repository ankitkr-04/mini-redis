package command;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
import store.DataStore;

public sealed interface ICommand permits PingCommand, EchoCommand, SetCommand, GetCommand,
        RPushCommand, LRangeCommand, LPushCommand, LLenCommand, LPopCommand, BLPopCommand {

    default ByteBuffer execute(String[] args, DataStore dataStore, SocketChannel channel) {
        return execute(args, dataStore);
    }

    boolean validateArgs(String[] args);

    ByteBuffer execute(String[] args, DataStore dataStore);

}
