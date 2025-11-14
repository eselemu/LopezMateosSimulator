package simulation.agents;

public class Agent extends Thread {
    public enum AgentType {
        CAR,
        PEDESTRIAN,
        SEMAPHORE
    }

    public enum AgentState {
        ACTIVE,
        WAITING,
        STOPPED,
        FINISHED
    }

    public AgentType type;
    public int id;
    protected AgentState state;
    protected volatile boolean running = true;

    public AgentState getAgentState() { return state; }

    public void stopAgent() {
        running = false;
        state = AgentState.STOPPED;
    }
}
