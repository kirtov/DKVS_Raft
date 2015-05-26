package raft.commands;

import raft.util.Entry;

import java.net.SocketAddress;

public abstract class Command {
    public Entry entry;
    public SocketAddress address;

    public Command(SocketAddress addr, Entry entry) {
        this.address = addr;
        this.entry = entry;
    }

    public String getMessageToSend(){
        return "empty";
    }

}
