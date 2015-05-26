package raft.commands;

import raft.util.Entry;

import java.net.SocketAddress;

public class AppendEntriesResult extends Command {
    public boolean success;
    public int id, logSize,term;

    public AppendEntriesResult(SocketAddress addr, Entry entry, int term, int id, int logSize, boolean success) {
        super(addr, entry);
        this.success = success;
        this.id = id;
        this.logSize = logSize;
        this.term = term;
    }

    public String getMessageToSend() {
        return "append_response " + term + " " + id + " " + logSize + " " + success + " ";
    }

    @Override
    public String toString() {
        return "AppendEntriesResult: id=" + id + " logSize=" + logSize + " success=" + success + " address: " + address;
    }
}
