package commands;

import java.nio.ByteBuffer;
import commands.list.LLenCommand;
import commands.list.LPushCommand;
import commands.list.LRangeCommand;
import commands.list.RPushCommand;
import commands.string.GetCommand;
import commands.string.SetCommand;
import commands.util.EchoCommand;
import commands.util.PingCommand;
import store.DataStore;

public sealed interface ICommand permits PingCommand, EchoCommand, SetCommand, GetCommand,
        RPushCommand, LRangeCommand, LPushCommand, LLenCommand {
    ByteBuffer execute(String[] args, DataStore dataStore);

    boolean validateArgs(String[] args);

}
