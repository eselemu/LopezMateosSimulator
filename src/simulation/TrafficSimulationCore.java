package simulation;

import simulation.agents.*;
import simulation.distributed.SemaphoreRegistry;
import simulation.distributed.DistributedSemaphoreClient;
import simulation.map.MapManager;
import simulation.map.TrafficNode;

import java.rmi.RemoteException;
import java.util.*;

public class TrafficSimulationCore {

    private static TrafficSimulationCore instance;
    public static int vehicleSpeed;
    private List<Car> cars;
    private List<Truck> trucks;
    private List<SemaphoreSimulation> semaphores;
    private List<Pedestrian> pedestrians;
    private boolean isRunning;
    private MapManager mapManager;
    public static volatile Map<Thread.State, Integer> counts;
    
    // Distributed mode components
    private SemaphoreRegistry semaphoreRegistry;
    private DistributedSemaphoreClient distributedClient;
    private boolean distributedModeEnabled;
    private int registryPort;
    
    private TrafficSimulationCore(){
        cars = new ArrayList<>();
        trucks = new ArrayList<>();
        semaphores = new ArrayList<>();
        pedestrians = new ArrayList<>();
        mapManager = MapManager.getInstance();
        distributedModeEnabled = false;
        registryPort = 1099; // Default RMI port
        semaphoreRegistry = SemaphoreRegistry.getInstance();
        distributedClient = DistributedSemaphoreClient.getInstance();
        vehicleSpeed = 750;
    }

    public static TrafficSimulationCore getInstance(){
        if(instance == null) instance = new TrafficSimulationCore();
        return instance;
    }

    // In the initializeSimulation method, add pedestrian creation:
    public void initializeSimulation(int carsNumber, int trucksNumber, int semaphoresNumber,
                                     int pedestriansNumber, int greenLightTimer,
                                     int yellowLightTimer, int redLightTimer, int speed) {

        vehicleSpeed = speed;
        mapManager.initializeSimpleMap();

        int[] lightsTimers = {greenLightTimer, yellowLightTimer, redLightTimer};
        SemaphoreSimulation.setLightsTimer(lightsTimers);

        // Create cars with dynamic routing
        for (int i = 0; i < carsNumber; i++) {
            TrafficNode startNode = mapManager.getRandomStartNode();
            TrafficNode endNode = mapManager.getRandomEndNode(startNode);

            if (startNode != null && endNode != null) {
                Car car = new Car(i+1, startNode.position, endNode.position);
                cars.add(car);
                System.out.println("Car " + (i+1) + " route: " + startNode.nodeId + " → " + endNode.nodeId);
            }
        }

        // Create trucks with dynamic routing
        for (int i = 0; i < trucksNumber; i++) {
            TrafficNode startNode = mapManager.getRandomStartNode();
            TrafficNode endNode = mapManager.getRandomEndNode(startNode);

            if (startNode != null && endNode != null) {
                Truck truck = new Truck(i+1, startNode.position, endNode.position);
                trucks.add(truck);
                System.out.println("Truck " + (i+1) + " route: " + startNode.nodeId + " → " + endNode.nodeId);
            }
        }

        // Create pedestrians
        for (int i = 0; i < pedestriansNumber; i++) {
            Pedestrian pedestrian = new Pedestrian(i+1);
            pedestrians.add(pedestrian);
            System.out.println("Pedestrian " + (i+1) + " created");
        }

        semaphores.addAll(mapManager.getAllSemaphores());
        
        // If distributed mode is already enabled, register new semaphores
        if (distributedModeEnabled && !semaphores.isEmpty()) {
            try {
                semaphoreRegistry.registerSemaphores(semaphores);
                System.out.println("✅ Auto-registered " + semaphores.size() + 
                                 " semaphores in distributed mode");
            } catch (RemoteException e) {
                System.err.println("⚠️ Failed to auto-register semaphores: " + e.getMessage());
            }
        }
    }

    // In the startSimulation method, add pedestrian start:
    public void startSimulation(){
        isRunning = true;

        // Start semaphores
        for(SemaphoreSimulation semaphore : semaphores){
            semaphore.start();
        }

        // Start cars
        for(Car car : cars){
            car.start();
        }

        for(Truck truck : trucks){
            truck.start();
        }

        // Start pedestrians
        for(Pedestrian pedestrian : pedestrians){
            pedestrian.start();
        }

        System.out.println("Simulación iniciada");
    }

    // In the stopSimulation method, add pedestrian stop:
    public void stopSimulation(){
        isRunning = false;

        for(Car car : cars){
            car.stopCar();
        }

        for(Truck truck : trucks){
            truck.stopTruck();
        }

        for(SemaphoreSimulation semaphore : semaphores){
            semaphore.stopSemaphore();
        }

        for(Pedestrian pedestrian : pedestrians){
            pedestrian.stopPedestrian();
        }

        // Note: We don't disconnect from distributed manager here
        // to allow reconnection. Call disconnectFromTrafficManager() explicitly if needed.

        System.out.println("Simulación detenida");
    }

    // Getters para UI
    public List<Car> getCars() { return cars; }
    public List<Truck> getTrucks() { return trucks; }
    public List<SemaphoreSimulation> getSemaphores() { return semaphores; }
    public List<Pedestrian> getPedestrians() { return pedestrians; }
    public boolean isRunning() { return isRunning; }

    /**
     * Connect to distributed traffic manager by starting RMI registry and registering semaphores.
     * This enables distributed mode where semaphores can be accessed remotely via RMI.
     * 
     * @param port The port for the RMI registry (default: 1099)
     * @return true if connection successful, false otherwise
     */
    public boolean connectToTrafficManager(int port) {
        if (distributedModeEnabled) {
            System.out.println("⚠️ Already connected to distributed traffic manager");
            return true;
        }

        try {
            this.registryPort = port > 0 ? port : 1099;
            
            // Start RMI registry
            semaphoreRegistry.startRegistry(registryPort);
            
            // Register all semaphores
            if (!semaphores.isEmpty()) {
                semaphoreRegistry.registerSemaphores(semaphores);
                System.out.println("✅ Registered " + semaphores.size() + 
                                 " semaphores in distributed mode");
            }
            
            // Configure distributed client
            distributedClient.configureDefaults("localhost", registryPort);
            
            distributedModeEnabled = true;
            System.out.println("✅ Connected to distributed traffic manager on port " + registryPort);
            return true;
            
        } catch (RemoteException e) {
            System.err.println("❌ Failed to connect to distributed traffic manager: " + e.getMessage());
            e.printStackTrace();
            distributedModeEnabled = false;
            return false;
        }
    }

    /**
     * Connect to distributed traffic manager using default port (1099)
     * @return true if connection successful, false otherwise
     */
    public boolean connectToTrafficManager() {
        return connectToTrafficManager(1099);
    }

    /**
     * Disconnect from distributed traffic manager by stopping RMI registry.
     * This disables distributed mode and unregisters all semaphores.
     */
    public void disconnectFromTrafficManager() {
        if (!distributedModeEnabled) {
            System.out.println("⚠️ Not connected to distributed traffic manager");
            return;
        }

        try {
            semaphoreRegistry.stopRegistry();
            distributedClient.clearCache();
            distributedModeEnabled = false;
            System.out.println("✅ Disconnected from distributed traffic manager");
        } catch (Exception e) {
            System.err.println("❌ Error disconnecting from distributed traffic manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if distributed mode is enabled
     * @return true if distributed mode is active
     */
    public boolean isDistributedModeEnabled() {
        return distributedModeEnabled;
    }

    /**
     * Get the RMI registry port
     * @return The port number
     */
    public int getRegistryPort() {
        return registryPort;
    }

    /**
     * Get statistics about distributed connections
     * @return Statistics string
     */
    public String getDistributedStatistics() {
        if (!distributedModeEnabled) {
            return "Distributed mode is not enabled";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Distributed Mode Statistics:\n");
        stats.append("  Registry Port: ").append(registryPort).append("\n");
        stats.append("  Registered Semaphores: ").append(semaphoreRegistry.getRegisteredCount()).append("\n");
        stats.append("\n").append(distributedClient.getStatistics());
        return stats.toString();
    }

    public  Map<Thread.State, Integer> getThreadStateCounts() {
        Map<Thread.State, Integer> counts = new HashMap<>();

        for (Car car : cars) {
            Thread.State state = car.getState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }

        for (SemaphoreSimulation sem : semaphores) {
            Thread.State state = sem.getState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }

        for (Pedestrian pedestrian : pedestrians) {
            Thread.State state = pedestrian.getState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }

        for (Truck truck : trucks) {
            Thread.State state = truck.getState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }

        return counts;
    }

    public Map<Car.CarState, Integer> getCarStateCounts() {
        Map<Car.CarState, Integer> counts = new HashMap<>();
        for (Car car : cars) {
            Car.CarState state = car.getCarState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }
        return counts;
    }

    public Map<SemaphoreSimulation.LightState, Integer> getSemStateCounts() {
        Map<SemaphoreSimulation.LightState, Integer> counts = new HashMap<>();
        for (SemaphoreSimulation sem : semaphores) {
            SemaphoreSimulation.LightState state = sem.getCurrentState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }
        return counts;
    }

    public Map<Pedestrian.PedestrianState, Integer> getPedestrianStateCounts() {
        Map<Pedestrian.PedestrianState, Integer> counts = new HashMap<>();
        for (Pedestrian pedestrian : pedestrians) {
            Pedestrian.PedestrianState state = pedestrian.getPedestrianState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }
        return counts;
    }

    public Map<Truck.TruckState, Integer> getTruckStateCounts() {
        Map<Truck.TruckState, Integer> counts = new HashMap<>();
        for (Truck truck : trucks) {
            Truck.TruckState state = truck.getTruckState();
            counts.put(state, counts.getOrDefault(state, 0) + 1);
        }
        return counts;
    }

    public Map<String, Integer> getAgentCount(){
        Map<String, Integer> counts = new HashMap<>();

        counts.put("Car", cars.size());
        counts.put("Truck", trucks.size());
        counts.put("Semaphore", semaphores.size());
        counts.put("Pedestrian", pedestrians.size());

        return counts;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    /**
     * Get the distributed semaphore client instance
     * @return The DistributedSemaphoreClient instance
     */
    public DistributedSemaphoreClient getDistributedClient() {
        return distributedClient;
    }

}
