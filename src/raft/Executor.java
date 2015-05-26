package raft;

import raft.commands.*;
import raft.util.Entry;
import raft.util.Log;
import raft.util.ServerInfo;
import raft.util.ServerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Executor extends Thread {

    ConcurrentLinkedQueue<Command> eventQueue;
    ServerInfo sInfo;
    Acceptor acceptor;
    StateMachine stateMachine;

    public Executor(ConcurrentLinkedQueue<Command> eventQueue, ServerInfo sInfo, Acceptor acceptor, StateMachine stateMachine) {
        this.eventQueue = eventQueue;
        this.sInfo = sInfo;
        this.acceptor = acceptor;
        this.stateMachine = stateMachine;
    }

    @Override
    public void run() {
        sInfo.lastReceiveTime = System.currentTimeMillis();
        while (true) {
            try {
                Command command = getNextCommand();
                if (command == null) {
                    verifyTimeout();
                } else {
                    if (command instanceof AppendEntriesRPC) {
                        onAppendEntriesRPC(command);
                    } else if (command instanceof RequestVoteRPC) {
                        onRequestVoteRPC(command);
                    } else if (command instanceof RequestVoteResult) {
                        onRequestVoteResult(command);
                    } else if (command instanceof AppendEntriesResult) {
                        onAppendEntriesResult(command);
                    } else if (command instanceof ClientRequest) {
                        if (sInfo.leader == -1) {
                            synchronized (eventQueue) {
                                eventQueue.add(command);
                            }
                        } else {
                            onClientRequest(command);
                        }
                    } else if (command instanceof ClientResponse) {
                        onClientResponse(command);
                    } else {
                        System.out.println("OOPS");
                    }
                }
            } catch (InterruptedException e) {
                closeAll();
                return;
            }
        }
    }

    private void closeAll() {
        try {
            stateMachine.fWriter.close();
            System.out.println("ADSDAS");
            interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Command getNextCommand() throws InterruptedException {
        int timeout;
        if (sInfo.getServerState() != ServerState.LEADER) {
            timeout = sInfo.timeout;;
        } else {
            timeout = sInfo.timeout / 2;
        }
        while (true) {
            synchronized (eventQueue) {
                if (!eventQueue.isEmpty()) {
                    return eventQueue.poll();
                } else if (System.currentTimeMillis() - sInfo.lastReceiveTime > 0.95*timeout) {
                    return null;
                } else {
                    eventQueue.wait(timeout - (System.currentTimeMillis() - sInfo.lastReceiveTime));
                }
            }

        }
    }

    public void onAppendEntriesResult(Command command) {
        AppendEntriesResult result = (AppendEntriesResult) command;
        if (sInfo.term < result.term) {
            if (sInfo.state != ServerState.FOLLOWER) {
                System.out.println("NEW STATE: FOLLOWER");
                sInfo.update();
                sInfo.setServerState(ServerState.FOLLOWER);
            }
            sInfo.setTerm(result.term);
            sInfo.lastReceiveTime = System.currentTimeMillis();
            if (sInfo.leader != result.id) {
                System.out.println("NEW LEADER: " + result.id);
            }
            sInfo.leader = result.id;
        }
        if (!result.success) {
            sInfo.nextIndex[result.id]--;
        } else {
            sInfo.nextIndex[result.id] = result.logSize;
            sInfo.matchIndex[result.id] = result.logSize;
            tryToCommit();
        }
    }

    private void tryToCommit() {
        for (int i = sInfo.commitIndex + 1; i < stateMachine.logEntries.size(); i++) {
            int commited = 1;
            for (int j = 1; j <= sInfo.N; j++) {
                if (sInfo.myId != j && sInfo.matchIndex[j] > i) {
                    commited++;
                }
            }
            if (commited >= (sInfo.N / 2 + 1) && stateMachine.logEntries.get(i).term == sInfo.term) {
                commit(i + 1);
                System.out.println("LOG " + (i + 1) + " WAS COMMITED");
            }
        }
    }

    private void commit(int newCommitIndex) {
        for (int i = sInfo.commitIndex + 1; i < newCommitIndex; i++) {
            ClientResponse response = null;
            try {
                response = stateMachine.commit(i);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (response != null) {
                acceptor.send(response);
            }
        }
    }

    public void onClientRequest(Command command) {
        ClientRequest request = (ClientRequest) command;
        switch (request.oper) {
            case PING:
                acceptor.send(new ClientResponse(request.address, null, "PONG", null));
                break;
            case GET:
                acceptor.send(new ClientResponse(request.address, null, stateMachine.getValue(Integer.parseInt(request.key)), null));
                break;
            case SET:
            case DELETE:
                if (sInfo.state == ServerState.LEADER) {
                    stateMachine.addToLog(command);
                    verifyTimeout();
                } else {
                    InetSocketAddress addrLeader = new InetSocketAddress(sInfo.hosts.get(sInfo.leader - 1), sInfo.ports.get(sInfo.leader - 1));
                    ClientRequest clientRequest;
                    if (request.id == -1) {
                        clientRequest = new ClientRequest(addrLeader, null, request.oper, request.key, request.value, sInfo.myId, (InetSocketAddress)request.address);
                    } else {
                        clientRequest = new ClientRequest(addrLeader, null, request.oper, request.key, request.value, request.id, request.clientAddr);
                    }
                    acceptor.send(clientRequest);
                }
        }
    }

    public void onClientResponse(Command command) {
        ClientResponse response = (ClientResponse) command;
        acceptor.send(new ClientResponse(response.clientAddr, null, response.answer, null));
    }

    public void onAppendEntriesRPC(Command command) {
        if (command.entry.term >= sInfo.term) {
            if (sInfo.state != ServerState.FOLLOWER) {
                System.out.println("NEW STATE: FOLLOWER");
                sInfo.update();
                sInfo.setServerState(ServerState.FOLLOWER);
            }
            if (sInfo.term != command.entry.term) {
                System.out.println("NEW TERM: " + command.entry.term);
            }
            sInfo.setTerm(command.entry.term);
            sInfo.lastReceiveTime = System.currentTimeMillis();
            if (sInfo.leader != command.entry.id) {
                System.out.println("NEW LEADER: " + command.entry.id);
            }
            sInfo.leader = command.entry.id;
        }
        if (sInfo.getTerm() == command.entry.term && stateMachine.valid(command.entry.prevLogIndex, command.entry.prevLogTerm)) {
            stateMachine.updateLogEntries(command.entry.prevLogIndex, ((AppendEntriesRPC)command).entries);
            acceptor.send(new AppendEntriesResult(command.address,null,sInfo.term, sInfo.myId, stateMachine.logEntries.size() ,true));
            if (command.entry.commitIndex > sInfo.commitIndex) {
                commit(Math.min(command.entry.commitIndex, stateMachine.logEntries.size()) + 1);
            }
        } else {
            acceptor.send(new AppendEntriesResult(command.address,null, sInfo.term, sInfo.getId(), stateMachine.logEntries.size() ,false));
        }
    }

    public void onRequestVoteRPC(Command command) {
        if (sInfo.term > command.entry.term) {
            acceptor.send(new RequestVoteResult(command.address, getEntry(), false));
        } else {
            if (sInfo.term < command.entry.term) {
                System.out.println("NEW STATE: FOLLOWER");
                sInfo.update();
                sInfo.setServerState(ServerState.FOLLOWER);
                sInfo.setTerm(command.entry.term);
                System.out.println("NEW TERM: " + sInfo.getTerm());
                sInfo.lastReceiveTime = System.currentTimeMillis();
            }
            if (uptodate(command.entry) && (sInfo.votedFor == -1 || sInfo.votedFor == command.entry.id)) {
                sInfo.setVotedFor(command.entry.id);
                acceptor.send(new RequestVoteResult(command.address, getEntry(), true));
            } else {
                acceptor.send(new RequestVoteResult(command.address, getEntry(), false));
            }
        }
    }

    private boolean uptodate(Entry cand) {
        return cand.prevLogTerm > stateMachine.getLlt() || cand.prevLogTerm == stateMachine.getLlt() && cand.prevLogIndex >= stateMachine.getLli();
    }

    public void onRequestVoteResult(Command command) {
        if (sInfo.state == ServerState.CANDIDATE) {
            int serverId = command.entry.id;
            boolean vote = ((RequestVoteResult) command).vote;
            if (vote) {
                if (sInfo.votedServers.get(serverId) == null || !sInfo.votedServers.get(serverId)) {
                    sInfo.voteCount++;
                    sInfo.votedServers.put(serverId, true);
                    int needVotes = Server.N / 2 + 1;
                    if (sInfo.voteCount >= needVotes) {
                        becomeLeader();
                    }
                }
            } else {
                sInfo.votedServers.put(serverId, false);
            }
        }
    }

    private void becomeLeader() {
        sInfo.setServerState(ServerState.LEADER);
        System.out.println("NEW ServerState: LEADER");
        Arrays.fill(sInfo.nextIndex, stateMachine.getLli() + 1);
        verifyTimeout();
    }

    public void verifyTimeout() {
        if (sInfo.state == ServerState.LEADER) {
            for (int i = 1; i <= sInfo.N; i++) {
                if (i != sInfo.myId) {
                    List<Log> entriesToSend = new ArrayList<>();
                    for (int j = 0; j < stateMachine.logEntries.size() - sInfo.nextIndex[i]; j++) {
                        entriesToSend.add(stateMachine.logEntries.get(sInfo.nextIndex[i] + j));
                    }
                    SocketAddress addr = new InetSocketAddress(sInfo.hosts.get(i - 1), sInfo.ports.get(i - 1));
                    acceptor.send(new AppendEntriesRPC(addr, new Entry(sInfo.term,sInfo.myId,sInfo.nextIndex[i] - 1,sInfo.nextIndex[i] == 0 ? -1 : stateMachine.logEntries.get(sInfo.nextIndex[i] - 1).term, sInfo.commitIndex), entriesToSend));
                }
            }
        } else {
            if (sInfo.state == ServerState.FOLLOWER) {
                System.out.println("NEW ServerState: CANDIDATE");
            }
            sInfo.leader = -1;
            sInfo.setTerm(sInfo.term + 1);
            System.out.println("NEW TERM: " + sInfo.getTerm());
            sInfo.state = ServerState.CANDIDATE;
            sInfo.votedServers.clear();
            sInfo.setVotedFor(sInfo.myId);
            sInfo.voteCount = 1;
            for (int i = 1; i <= sInfo.N; i++) {
                if (i != sInfo.myId) {
                    SocketAddress addr = new InetSocketAddress(sInfo.hosts.get(i - 1), sInfo.ports.get(i - 1));
                    Entry entry = getEntry();
                    acceptor.send(new RequestVoteRPC(addr, entry));
                }
            }
        }
        sInfo.lastReceiveTime = System.currentTimeMillis();
    }

    private Entry getEntry() {
        return new Entry(sInfo.term, sInfo.myId, stateMachine.getLli(), stateMachine.getLlt(), sInfo.commitIndex);
    }
}
