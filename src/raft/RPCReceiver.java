package raft;

import raft.commands.*;
import raft.util.Entry;
import raft.util.Log;
import raft.util.Operation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RPCReceiver extends Thread {
    private Scanner input;
    private ConcurrentLinkedQueue<Command> eventQueue;
    private Socket socket;

    public RPCReceiver(Socket socket, ConcurrentLinkedQueue<Command> eventQueue) {
        this.eventQueue = eventQueue;
        this.socket = socket;
        try {
            input = new Scanner(socket.getInputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Command request = getRequest();
                if (request != null) {
                    synchronized (eventQueue) {
                        eventQueue.add(request);
                        eventQueue.notify();
                    }
                }
            } catch (NoSuchElementException | IllegalStateException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private Command getRequest() throws NoSuchElementException {
        String token = input.next();
        Command curCommand = null;
        Entry entry;
        int id,count,term;
        Operation op;
        InetSocketAddress addr;
        switch (token) {
            case "vote_request":
                entry = new Entry(input.nextInt(),input.nextInt(),input.nextInt(),input.nextInt(), input.nextInt());
                curCommand = new RequestVoteRPC(socket.getRemoteSocketAddress(), entry);
                break;
            case "vote_response":
                term = input.nextInt();
                id = input.nextInt();
                int lli = input.nextInt();
                int llt = input.nextInt();
                int ci = input.nextInt();
                boolean vote = input.nextBoolean();
                entry = new Entry(term,id, lli, llt,ci);
                curCommand = new RequestVoteResult(socket.getRemoteSocketAddress(), entry, vote);
                break;
            case "append_request":
                entry = new Entry(Integer.parseInt(input.next()),input.nextInt(),input.nextInt(),input.nextInt(), input.nextInt());
                count = input.nextInt();
                List<Log> logEntries = new ArrayList<>();
                Log temp;
                for (int i = 0; i < count; i++) {
                    term = input.nextInt();
                    op = Operation.valueOf(input.next());
                    if (op == Operation.SET) {
                        temp = new Log(term, op, input.next(), input.next(), null);
                    } else {
                        temp = new Log(term ,op, input.next(), null, null);
                    }
                    logEntries.add(temp);
                }
                curCommand = new AppendEntriesRPC(socket.getRemoteSocketAddress(), entry, logEntries);
                break;
            case "append_response":
                term = input.nextInt();
                id = input.nextInt();
                int logSize = input.nextInt();
                boolean success = input.nextBoolean();
                curCommand = new AppendEntriesResult(socket.getRemoteSocketAddress(), null, term, id, logSize, success);
                break;
            case "client_request":
                Operation oper = Operation.valueOf(input.next());
                String key = null;
                String value = null;
                id = -1;
                if (oper == Operation.GET) {
                    key = input.next();
                } else if (oper == Operation.DELETE) {
                    key = input.next();
                } else if (oper == Operation.SET) {
                    key = input.next();
                    value = input.next();
                } else if (oper == Operation.PING) {

                }
                count = input.nextInt();
                addr = null;
                if (count == 2) {
                    id = input.nextInt();
                    addr = new InetSocketAddress(input.next(), input.nextInt());
                }
                curCommand = new ClientRequest(socket.getRemoteSocketAddress(), null,oper,key,value,id,addr);
                break;
            case "client_response":
                String answer = input.next();
                count = input.nextInt();
                addr = null;
                if (count == 1) {
                    addr = new InetSocketAddress(input.next(), input.nextInt());
                }
                curCommand = new ClientResponse(socket.getRemoteSocketAddress(), null,answer,addr);
                break;
            default:
                curCommand = null;
        }
        if (curCommand != null) {
            System.out.println("Receiving message " + curCommand.toString());
        } else {
            System.out.println("UNKNOWN");
        }
        return curCommand;
    }


}

