package simulation.map;

import java.util.concurrent.locks.ReentrantLock;

public class TrafficEdge {
    private TrafficNode from, to;
    private int capacity;
    private int currentOccupancy;
    private double length;
    private String edgeId;
    private ReentrantLock edgeLock;
    private boolean hasSidewalk;

    public TrafficEdge(String edgeId, TrafficNode from, TrafficNode to, double length) {
        this.edgeId = edgeId;
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.length = length;
        this.currentOccupancy = 0;
        this.edgeLock = new ReentrantLock();
        this.hasSidewalk = true; // All edges have sidewalks by default
    }

    // Getters
    public TrafficNode getFrom() { return from; }
    public TrafficNode getTo() { return to; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public double getLength() { return length; }
    public String getEdgeId() { return edgeId; }
    public boolean hasSidewalk() { return hasSidewalk; }
}