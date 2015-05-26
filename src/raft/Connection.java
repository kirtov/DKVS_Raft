package raft;


import raft.commands.Command;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Connection {

    RPCSender sender;
    RPCReceiver receiver;
    Socket socket;
    ConcurrentLinkedQueue<Command> eventQueue;
    public boolean isClosed;

    public Connection(Socket socket, ConcurrentLinkedQueue<Command> eventQueue) {
        this.socket = socket;
        this.eventQueue = eventQueue;
        isClosed = false;
        sender = new RPCSender(socket);
        receiver = new RPCReceiver(socket, eventQueue);
    }

    public void enableConnection() {
        sender.start();
        receiver.start();
    }

    public void close() {
        try {
            socket.close();
            isClosed = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return socket.getRemoteSocketAddress().toString();
    }
}
