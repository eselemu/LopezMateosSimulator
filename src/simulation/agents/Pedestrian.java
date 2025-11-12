package simulation.agents;

public class Pedestrian extends Agent {
    public Pedestrian(int id){
        this.id = id;
        type = AgentType.PEDESTRIAN;
    }

    @Override
    public void run(){
    }
}
