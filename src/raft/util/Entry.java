package raft.util;


public class Entry {
    public int id;
    public int term;
    public int prevLogIndex;
    public int prevLogTerm;
    public int commitIndex;


    public Entry(int term, int id, int lli, int llt, int ci) {
        this.id = id;
        this.term = term;
        this.prevLogIndex = lli;
        this.prevLogTerm = llt;
        this.commitIndex = ci;
    }

    public Entry(int term, int id, int lli, int llt) {
        this.id = id;
        this.term = term;
        this.prevLogIndex = lli;
        this.prevLogTerm = llt;
        commitIndex = -1;
    }

    @Override
    public String toString() {
        return term + " " + id + " " + prevLogIndex + " " + prevLogTerm + " " + commitIndex + " ";
    }

}
