package simulation;

public class TrafficSimulationCore {

    private static TrafficSimulationCore instance;

    private TrafficSimulationCore(){
    }

    public static TrafficSimulationCore getInstance(){
        if(instance == null) instance = new TrafficSimulationCore();
        return instance;
    }

    public void initializeSimulation(){}
    public void startSimulation(){}
    public void stopSimulation(){}
    public void connectToTrafficManager(){}
    public void disconnectFromTrafficManager(){}
}
