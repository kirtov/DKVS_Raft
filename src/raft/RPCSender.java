package raft;

import raft.commands.Command;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

public class RPCSender extends Thread {
    private PrintWriter output;
    private Queue<Command> q;
    Socket socket;

    public RPCSender(Socket socket) {
        q = new ArrayDeque<Command>();
        this.socket = socket;

        try {
            output = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(Command command) {
        q.add(command);
        synchronized (q) {
            q.notify();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (!q.isEmpty()) {
                Command next = q.poll();
                output.write(next.getMessageToSend());
                output.flush();
            } else {
                try {
                    synchronized (q) {
                        q.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



}

