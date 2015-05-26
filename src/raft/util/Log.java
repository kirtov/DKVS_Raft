package raft.util;

import raft.commands.ClientRequest;

public class Log {

    public Operation operation;
    public int term;
    public String key, value;
    public ClientRequest clientRequest;

    public Log(int term, Operation op, String key, String value, ClientRequest clientRequest) {
        this.operation = op;
        this.term = term;
        this.key = key;
        this.value = value;
        this.clientRequest = clientRequest;
    }

    public String getMessageToWrite() {
        return term + " " + operation + " " + ((key == null) ? "" : key) + " " + ((value == null) ? "" : value) + " ";
    }

    @Override
    public String toString() {
        return "LogEntry: term=" + term + " operation=" + operation + " key=" + ((key == null) ? "-" : key) + " value=" + ((value == null) ? "-" : value);
    }



}
