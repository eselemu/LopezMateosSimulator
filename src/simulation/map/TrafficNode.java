package simulation.map;

import simulation.agents.Agent;
import simulation.agents.SemaphoreSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class TrafficNode {
    public enum NodeType {
        INTERSECTION,
        STREET,
        CROSSWALK
    }

    public String nodeId;
    public Position position;
    private NodeType type;
    private List<TrafficEdge> outgoingEdges;
    private List<TrafficEdge> incomingEdges;
    private ReentrantLock nodeLock;
    private volatile Agent occupyingAgent;
    private SemaphoreSimulation semaphore;

    public TrafficNode(String nodeId, Position position, NodeType type) {
        this.nodeId = nodeId;
        this.position = position;
        this.type = type;
        this.outgoingEdges = new ArrayList<>();
        this.incomingEdges = new ArrayList<>();
        this.nodeLock = new ReentrantLock();
    }

    public boolean tryAcquire(Agent agent) {
        if (nodeLock.tryLock()) {
            if (occupyingAgent == null) {
                occupyingAgent = agent;
                return true;
            } else {
                nodeLock.unlock();
            }
        }
        return false;
    }

    public void release() {
        if (nodeLock.isHeldByCurrentThread()) {
            occupyingAgent = null;
            nodeLock.unlock();
        }
    }

    public void setOccupyingAgent(Agent agent) {
        this.occupyingAgent = agent;
    }

    public void addOutgoingEdge(TrafficEdge edge) {
        outgoingEdges.add(edge);
    }

    public void addIncomingEdge(TrafficEdge edge) {
        incomingEdges.add(edge);
    }

    // Getters
    public List<TrafficEdge> getOutgoingEdges() { return outgoingEdges; }
    public List<TrafficEdge> getIncomingEdges() { return incomingEdges; }
    public NodeType getType() { return type; }
    public SemaphoreSimulation getSemaphore() { return semaphore; }
    public void setSemaphore(SemaphoreSimulation semaphore) { this.semaphore = semaphore; }
    public String getTrafficNodeId() { return nodeId; }

    public int getCurrentOccupancy() {
        if (occupyingAgent == null) {
            return 0;
        }else {
            return 1;
        }
    }

    public NodeType getNodeType() { return type; }
}
