package raft.commands;

import raft.util.Entry;

import java.net.SocketAddress;

/**
 * Created by Kirill on 23.05.2015.
 */
public class RequestVoteRPC extends Command {

    public RequestVoteRPC(SocketAddress addr, Entry entry) {
        super(addr, entry);
    }

    public String getMessageToSend() {
        return "vote_request " + entry;
    }

    @Override
    public String toString() {
        return "RequestVoteRPC " +  entry + " address: " + address;
    }
}
