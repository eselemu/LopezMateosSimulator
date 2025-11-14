package simulation.map;

import java.util.concurrent.ConcurrentHashMap;

public class PositionRegistry {
    private ConcurrentHashMap<String, Position> agentPositions;
    private ConcurrentHashMap<Position, String> positionAgents;

    public PositionRegistry() {
        agentPositions = new ConcurrentHashMap<>();
        positionAgents = new ConcurrentHashMap<>();
    }

    public boolean registerPosition(String agentId, Position pos) {
        if(positionAgents.putIfAbsent(pos, agentId) == null) {
            agentPositions.put(agentId, pos);
            return true;
        }
        return false;
    }

    public boolean moveAgent(String agentId, Position newPos) {
        Position oldPos = agentPositions.get(agentId);
        if(oldPos != null) {
            positionAgents.remove(oldPos);
        }

        if(positionAgents.putIfAbsent(newPos, agentId) == null) {
            agentPositions.put(agentId, newPos);
            return true;
        }
        return false;
    }

    public Position getAgentPosition(String agentId) {
        return agentPositions.get(agentId);
    }
}