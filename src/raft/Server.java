package raft;

import raft.commands.Command;
import raft.util.ServerInfo;
import raft.util.ServerState;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    public static int N;
    private static int PORT;
    private static int myNumber;
    private static int timeout;
    private static ServerInfo myInfo;

    private static ConcurrentLinkedQueue<Command> eventQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws IOException {
        try {
            myNumber = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        init();
        startServer();
    }

    private static void startServer() throws IOException {
        eventQueue = new ConcurrentLinkedQueue<>();
        ServerSocket socket = new ServerSocket(PORT);
        Acceptor acceptor = new Acceptor(socket, eventQueue);
        StateMachine stateMachine = new StateMachine(myInfo);
        Executor executor = new Executor(eventQueue, myInfo, acceptor, stateMachine);
        acceptor.start();
        executor.start();
    }

    private static void init() throws IOException {
        myInfo = new ServerInfo(myNumber,1,ServerState.FOLLOWER,-1);
        if (myNumber == 1) {
            myInfo.setServerState(ServerState.LEADER);
        }
        initCluster();
        System.out.println("Server was initialized: " + myInfo + " PORT=" +PORT);
    }


    public static void initCluster() {
        String result = "";
        Properties prop = new Properties();
        String propFileName = "dkvs.properties";
        ArrayList<String> hosts = new ArrayList<>();
        ArrayList<Integer> ports = new ArrayList<>();
        try {
            FileInputStream in = new FileInputStream(propFileName);
            try {
                prop.load(in);
                String s ="";
                int i = 1;
                do {
                    s = prop.getProperty("node."+i);
                    if (s != null) {
                        String[] all = s.split(":");
                        hosts.add(all[0]);
                        ports.add(Integer.parseInt(all[1]));

                        i++;
                    }
                } while( s != null);
                N = i - 1;
                PORT = ports.get(myNumber - 1);
                myInfo.N = N;
                timeout = Integer.parseInt(prop.getProperty("timeout"));
                myInfo.timeout = timeout;
                myInfo.initCluster(hosts, ports);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
