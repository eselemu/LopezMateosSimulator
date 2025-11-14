package simulation.map;

import simulation.agents.Agent;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class TrafficNode {

    public enum NodeType{
        INTERSECTION,
        STREET,
        CROSSWALK
    }

    public String nodeId;
    public Position position;
    private NodeType type;
    private List<TrafficNode> adjacentNodes;
    private ReentrantLock nodeLock;
    private Agent occupyingAgent;
    private int capacity;
}
