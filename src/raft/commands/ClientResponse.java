package raft.commands;

import raft.util.Entry;
import raft.util.Operation;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by Kirill on 25.05.2015.
 */
public class ClientResponse extends Command {
    public String answer;
    public InetSocketAddress clientAddr;

    public ClientResponse(SocketAddress addr, Entry entry, String answer, InetSocketAddress clientAddr) {
        super(addr, entry);
        this.answer = answer;
        this.clientAddr = clientAddr;
    }

    public String getMessageToSend() {
        return "client_response " + answer + " " + (clientAddr == null ? (0) : (1 + " " +  clientAddr.getHostName() + " " + clientAddr.getPort())) + " ";
    }

    @Override
    public String toString() {
        return "ClientResponse: Answer=" + answer + " " + (clientAddr == null ? (0) : (1 + " toClient=" +  clientAddr.getHostName() + ":" + clientAddr.getPort()));
    }
}
