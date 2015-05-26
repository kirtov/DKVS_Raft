package raft.util;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public  class ServerInfo {

    public ArrayList<String> hosts;
    public ArrayList<Integer> ports;
    public int N;
    public int myId;
    public int leader = 1;
    public int term = 1;
    public int lsn = 0;
    public int votedFor = -1;
    public ServerState state = ServerState.FOLLOWER;
    public Map<Integer, Boolean> votedServers;
    public int voteCount = 0;
    public long lastReceiveTime = System.currentTimeMillis();
    public int timeout;
    public int[] nextIndex, matchIndex;
    public int commitIndex;

    public ServerInfo(int myId, int lead,  ServerState st, int votedFor) {
        this.myId = myId;
        setLeader(lead);
        setServerState(st);
        setVotedFor(votedFor);
        votedServers = new HashMap<>();

    }

    public void initCluster(ArrayList<String> hosts, ArrayList<Integer> ports) {
        this.hosts = hosts;
        this.ports = ports;
    }

    public void nextTerm() {
        term++;
        update();
    }

    public void setN(int N) {
        this.N = N;
        nextIndex = new int[N + 1];
        matchIndex = new int[N + 1];
    }
    public void update() {
        votedFor = -1;
        votedServers.clear();
        voteCount = 0;
    }

    public int getId() {
        return myId;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setVotedFor(int vf) {
        this.votedFor = vf;
    }

    public void setLeader(int leader) {
        this.leader = leader;
    }

    public void setServerState(ServerState ServerState) {
        this.state = ServerState;
    }

    public void setLsn(int lsn) {
        this.lsn = lsn;
    }

    public int getLeader() {
        return leader;
    }

    public int getTerm() {
        return term;
    }

    public int getVotedFor() {
        return votedFor;
    }

    public int getLsn() {
        return lsn;
    }

    public ServerState getServerState() {
        return state;
    }

    @Override
    public String toString() {
        return "number=" + myId + " term=" + term + " leader=" + leader;
    }

}