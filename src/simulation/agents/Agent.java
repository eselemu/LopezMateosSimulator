package simulation.agents;

public class Agent extends Thread {
    public enum AgentType {
        CAR,
        PEDESTRIAN,
        SEMAPHORE
    }
    public AgentType type;

    public int id;
}
