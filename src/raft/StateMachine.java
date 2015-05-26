package raft;

import raft.commands.ClientRequest;
import raft.commands.ClientResponse;
import raft.commands.Command;
import raft.util.Log;
import raft.util.Operation;
import raft.util.ServerInfo;

import java.io.*;
import java.util.*;

public class StateMachine {

    public List<Log> logEntries;
    public Map<Integer, String> dataMap;
    private ServerInfo sInfo;

    public FileWriter fWriter;

    protected void finalize() throws Throwable {
        System.out.println("finalize");
        fWriter.close();
    }

    public StateMachine(ServerInfo sInfo) throws IOException {
        String logName = "dkvs_" + sInfo.myId + ".log";
        this.sInfo = sInfo;
        dataMap = new HashMap<>();
        logEntries = new ArrayList<>();
        readFromLog(logName);
        fWriter = new FileWriter(logName, true);
        //writeBack();
    }

    private void writeBack() throws IOException {
        for (int i = 0; i < logEntries.size(); i++) {
            fWriter.write(logEntries.get(i).getMessageToWrite() + "\n");
            System.out.println(logEntries.get(i));
            fWriter.flush();
        }
    }

    private void readFromLog(String logName) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileReader(logName));
        int term;
        String key = null, value = null;
        Operation operation;
        while (sc.hasNext()) {
            term = sc.nextInt();
            operation = Operation.valueOf(sc.next());
            if (operation == Operation.SET) {
                key = sc.next();
                value = sc.next();
            } else if (operation == Operation.DELETE) {
                key = sc.next();
            }
            Log nextLog = new Log(term, operation, key, value, null);
            logEntries.add(nextLog);
            if (nextLog.operation == Operation.SET) {
                dataMap.put(Integer.valueOf(nextLog.key), nextLog.value);
            } else {
                if (dataMap.containsKey(nextLog.key)) {
                    dataMap.remove(Integer.valueOf(nextLog.key));
                }
            }
        }
        sInfo.commitIndex = logEntries.size() - 1;
        sInfo.nextIndex = new int[sInfo.N + 2];
        sInfo.matchIndex = new int[sInfo.N + 2];
        int llt = getLlt();
        if (llt == -1) llt = 1;
        sInfo.setTerm(llt);
        System.out.println("TERM FROM LOG=" + sInfo.getTerm());
        Arrays.fill(sInfo.nextIndex, getLli() + 1);
        sc.close();
    }

    public void addToLog(Command command) {
        ClientRequest request = (ClientRequest) command;
        Log le = new Log(sInfo.getTerm(),request.oper,request.key, request.value, request);
        logEntries.add(le);
    }

    public int getLlt() {
        if (logEntries.size() != 0) {
            return logEntries.get(logEntries.size() - 1).term;
        } else {
            return -1;
        }
    }

    public int getLli() {
        return Math.max(-1, logEntries.size() - 2);
    }

    public void updateLogEntries(int prevLogIndex, List<Log> newEntries) {
        while (logEntries.size() > prevLogIndex + 1) {
            logEntries.remove(logEntries.size() - 1);
        }
        for (int i = 0; i < newEntries.size(); i++) {
            logEntries.add(newEntries.get(i));
            System.out.println("APPLYING NEW LOG " + newEntries.get(i));
        }
    }

    public boolean valid(int prevLogIndex, int prevLogTerm) {
        if (prevLogIndex == -1 || (logEntries.size() > prevLogIndex && logEntries.get(prevLogIndex).term == prevLogTerm)) {
            return true;
        } else {
            return false;
        }
    }

    public String getValue(int key) {
        if (dataMap.containsKey(key)) {
            return "VALUE " + key + " " + dataMap.get(key);
        } else {
            return "NOT_FOUND";
        }
    }


    public ClientResponse commit(int index) throws IOException {
        Log logToCommit = logEntries.get(index);
        ClientResponse clientResponse = null;
        String answer = null;
        if (logToCommit.operation == Operation.SET) {
            dataMap.put(Integer.valueOf(logToCommit.key), logToCommit.value);
            fWriter.write(logToCommit.getMessageToWrite() + "\n");
            fWriter.flush();
            answer = "STORED";
        }
        if (logToCommit.operation == Operation.DELETE) {
            if (dataMap.containsKey(Integer.valueOf(logToCommit.key))) {
                dataMap.remove(Integer.valueOf(logToCommit.key));
                answer = "DELETED";
                fWriter.write(logToCommit.getMessageToWrite() + "\n");
                fWriter.flush();
            } else {
                answer = "NOT_FOUND";
            }
        }
        if (logToCommit.clientRequest != null) {
            clientResponse = new ClientResponse(logToCommit.clientRequest.address, null, answer, logToCommit.clientRequest.clientAddr);
        }
        sInfo.commitIndex++;
        return clientResponse;
    }

}
