package commands;

import java.nio.ByteBuffer;
import store.DataStore;

public sealed interface ICommand
        permits PingCommand, EchoCommand, SetCommand, GetCommand, RPushCommand, LRangeCommand {
    ByteBuffer execute(String[] args, DataStore dataStore);

    boolean validateArgs(String[] args);

}
