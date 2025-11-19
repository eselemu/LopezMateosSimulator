package simulation;

import simulation.agents.Agent;
import simulation.agents.Car;
import simulation.agents.Pedestrian;
import simulation.agents.SemaphoreSimulation;
import simulation.map.MapManager;
import simulation.map.Position;

import java.util.*;

public class TrafficSimulationCore {

    private static TrafficSimulationCore instance;
    private List<Car> cars;
    private List<SemaphoreSimulation> semaphores;
    private List<Pedestrian> pedestrians;
    private boolean isRunning;
    private MapManager mapManager;
    public static volatile Map<Thread.State, Integer> counts;

    private TrafficSimulationCore(){
        cars = new ArrayList<>();
        semaphores = new ArrayList<>();
        pedestrians = new ArrayList<>();
        mapManager = MapManager.getInstance();
    }

    public static TrafficSimulationCore getInstance(){
        if(instance == null) instance = new TrafficSimulationCore();
        return instance;
    }

    public void initializeSimulation(int carsNumber, int semaphoresNumber, int pedestriansNumber){
        // Crear mapa simple: 2 bloques con semáforo en medio
        mapManager.initializeSimpleMap();

        Random random = new Random();
        int maxInt = 9;

        for (int i = 0; i < carsNumber; i++) {
            Car car = new Car(i+1, new Position(0, 0), new Position(random.nextInt(6,9), 0));
            cars.add(car);
        }

        for (int i = 0; i < semaphoresNumber; i++) {
            SemaphoreSimulation semaphore = new SemaphoreSimulation(i+1, new Position(random.nextInt(2,9), 0));
            semaphores.add(semaphore);
            mapManager.registerSemaphore(semaphore);
        }

        // Crear semáforo en posición (5, 0)
//        SemaphoreSimulation semaphore = new SemaphoreSimulation(1, new Position(5, 0));
//        semaphores.add(semaphore);
//
//        // Crear 2 carros en posiciones iniciales
//        Car car1 = new Car(1, new Position(0, 0), new Position(8, 0));
//        Car car2 = new Car(2, new Position(0, 0), new Position(7, 0));
//        Car car3 = new Car(3, new Position(0, 0), new Position(6, 0));
//
//        cars.add(car1);
//        cars.add(car2);
//        cars.add(car3);

}

    public void startSimulation(){
        isRunning = true;

        // Iniciar semáforos
        for(SemaphoreSimulation semaphore : semaphores){
            semaphore.start();
        }

        // Iniciar carros
        for(Car car : cars){
            car.start();
        }

//        for(Pedestrian pedestrian : pedestrians){
//            pedestrian.start();
//        }

        System.out.println("Simulación iniciada");
    }

    public void stopSimulation(){
        isRunning = false;

        for(Car car : cars){
            car.stopCar();
        }

        for(SemaphoreSimulation semaphore : semaphores){
            semaphore.stopSemaphore();
        }

        System.out.println("Simulación detenida");
    }

    // Getters para UI
    public List<Car> getCars() { return cars; }
    public List<SemaphoreSimulation> getSemaphores() { return semaphores; }
    public boolean isRunning() { return isRunning; }

    public void connectToTrafficManager(){}
    public void disconnectFromTrafficManager(){}

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

//        for (Pedestrian pedestrian : pedestrians) {
//            Thread.State state = pedestrian.getState();
//            counts.put(state, counts.getOrDefault(state, 0) + 1);
//        }

        return counts;
    }

    public Map<String, Integer> getAgentCount(){
        Map<String, Integer> counts = new HashMap<>();

        counts.put("Car", cars.size());
        counts.put("Semaphore", semaphores.size());
        // counts.put("Pedestrian", pedestrians.size());

        return counts;
    }

}
