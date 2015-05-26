package raft.commands;

import raft.util.Entry;

import java.net.SocketAddress;

public class RequestVoteResult extends Command {
    public boolean vote;

    public RequestVoteResult(SocketAddress addr, Entry entry, boolean vote) {
        super(addr, entry);
        this.vote = vote;
    }

    public String getMessageToSend() {
        return "vote_response " + entry + " " + vote + " ";
    }

    @Override
    public String toString() {
        return "RequestVoteResult " +  entry + vote + " address: " + address;
    }

}
