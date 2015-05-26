package raft;

import raft.commands.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Acceptor extends Thread {

    private ServerSocket serverSocket;
    private HashMap<SocketAddress, Connection> connections;
    ConcurrentLinkedQueue<Command> eventQueue;

    public Acceptor(ServerSocket ssocket, ConcurrentLinkedQueue<Command> eQueue) {
        serverSocket = ssocket;
        this.eventQueue = eQueue;
        connections = new HashMap<>();
    }

    public void send(Command command) {
        Connection to;
        if (!connections.containsKey(command.address) || connections.get(command.address).isClosed) {
            Socket socket = new Socket();
            try {
                socket.connect(command.address);
                to = new Connection(socket, eventQueue);
                connections.put(command.address, to);
                to.enableConnection();
            } catch (IOException e) {
                System.out.println("Failed to connect to " + command.address);
            }
        }
        to = connections.get(command.address);

        if (to != null && !to.isClosed) {
            to.sender.send(command);
            System.out.println("Sending message " + command.toString());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                SocketAddress address = socket.getRemoteSocketAddress();
                if (connections.containsKey(address)) {
                    connections.get(address).close();
                }
                Connection connection = new Connection(socket, eventQueue);
                connections.put(address, connection);
                connection.enableConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}