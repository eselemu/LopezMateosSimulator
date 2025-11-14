package simulation;

import simulation.agents.Car;
import simulation.agents.SemaphoreSimulation;
import simulation.map.MapManager;
import simulation.map.Position;

import java.util.ArrayList;
import java.util.List;

public class TrafficSimulationCore {

    private static TrafficSimulationCore instance;
    private List<Car> cars;
    private List<SemaphoreSimulation> semaphores;
    private boolean isRunning;
    private MapManager mapManager;

    private TrafficSimulationCore(){
        cars = new ArrayList<>();
        semaphores = new ArrayList<>();
        mapManager = MapManager.getInstance();
    }

    public static TrafficSimulationCore getInstance(){
        if(instance == null) instance = new TrafficSimulationCore();
        return instance;
    }

    public void initializeSimulation(){
        // Crear mapa simple: 2 bloques con semáforo en medio
        mapManager.initializeSimpleMap();

        // Crear semáforo en posición (5, 0)
        SemaphoreSimulation semaphore = new SemaphoreSimulation(1, new Position(5, 0));
        semaphores.add(semaphore);

        // Crear 2 carros en posiciones iniciales
        Car car1 = new Car(1, new Position(0, 0), new Position(8, 0));
        Car car2 = new Car(2, new Position(0, 0), new Position(7, 0));

        cars.add(car1);
        cars.add(car2);

        System.out.println("Simulación inicializada con 2 carros y 1 semáforo");
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
}
