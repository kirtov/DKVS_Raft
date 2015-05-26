package raft.commands;

import raft.util.Entry;
import raft.util.Operation;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by Kirill on 25.05.2015.
 */
public class ClientRequest extends Command {
    public Operation oper;
    public int id;
    public String key, value;
    public InetSocketAddress clientAddr;

    public ClientRequest(SocketAddress addr, Entry entry, Operation oper, String key, String value, int id, InetSocketAddress clientAddr) {
        super(addr, entry);
        this.id = id;
        this.key = key;
        this.value = value;
        this.clientAddr = clientAddr;
        this.oper = oper;
    }

    public String getMessageToSend() {
        return "client_request " + oper + " " + ((key == null) ? "" : key + " ") + ((value == null) ? "" : value + " ") + (id == -1 ? (0) : (2 + " " + id + " " + clientAddr.getHostName() + " " + clientAddr.getPort())) + " ";
    }

    @Override
    public String toString() {
        return "ClientRequest: Operation=" + oper + " key=" + ((key == null) ? "-" : key) + " value=" + ((value == null) ? "-" : value) + (id == -1 ? (" " + 0) : (" " + 2 + " fromId=" + id + " toClient=" +  clientAddr.getHostName() + ":" + clientAddr.getPort()));
    }
}
