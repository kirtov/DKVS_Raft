package raft.commands;

import raft.util.Entry;
import raft.util.Log;
import raft.util.Operation;

import java.net.SocketAddress;
import java.util.List;

public class AppendEntriesRPC extends Command {
    public List<Log> entries;

    public AppendEntriesRPC(SocketAddress addr, Entry entry, List<Log> entries) {
        super(addr, entry);
        this.entries = entries;
    }

    public String getMessageToSend() {
        StringBuilder sb = new StringBuilder("append_request " + entry + entries.size() + " ");
        for (int i = 0; i < entries.size(); i++) {
            sb.append(entries.get(i).getMessageToWrite() + " ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AppendEntriesRPC: entry=" + entry + entries.size() + " ");
        for (int i = 0; i < entries.size(); i++) {
            sb.append(entries.get(i).getMessageToWrite() + " ");
        }
        return sb.toString() + " address=" + address;
    }
}
