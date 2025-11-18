package simulation.map;

import simulation.agents.Car;
import simulation.agents.SemaphoreSimulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapManager {
    private static MapManager instance;
    private TrafficMap trafficMap;
    private PositionRegistry positionRegistry;
    private Map<String, StreetSegment> streetSegments;
    private Map<Position, SemaphoreSimulation> semaphorePositions;

    private MapManager() {
        streetSegments = new HashMap<>();
        semaphorePositions = new HashMap<>();
        positionRegistry = new PositionRegistry();
    }

    public static MapManager getInstance() {
        if (instance == null) instance = new MapManager();
        return instance;
    }

    public void initializeSimpleMap() {
        // Crear segmentos de calle simples para 2 bloques
        // Bloque 1: posiciones 0-4
        for(int i = 0; i < 10; i++) {
            String segmentId = "street_" + i + "_" + (i+1);
            StreetSegment segment = new StreetSegment(segmentId, 1); // Capacidad 1
            streetSegments.put(segmentId, segment);
        }

        // Bloque 2: posiciones 5-9
        /*for(int i = 5; i < 9; i++) {
            String segmentId = "street_" + i + "_" + (i+1);
            StreetSegment segment = new StreetSegment(segmentId, 1); // Capacidad 1
            streetSegments.put(segmentId, segment);
        }*/
    }

    public StreetSegment getStreetSegment(Position from, Position to) {
        String segmentId = "street_" + from.x + "_" + to.x;
        return streetSegments.get(segmentId);
    }

    public void moveCar(Car car, Position from, Position to) {
        positionRegistry.moveAgent("car_" + car.id, to);
    }

    public SemaphoreSimulation getSemaphoreAt(Position position) {
        SemaphoreSimulation sem = semaphorePositions.get(position);
        return sem;
    }

    public void registerSemaphore(SemaphoreSimulation semaphore) {
        semaphorePositions.put(semaphore.getPosition(), semaphore);
    }

    public Map<StreetSegment, Integer> getStreetSegments() {
        Map<StreetSegment, Integer> streetCount = new HashMap<>();

        for (StreetSegment segment : streetSegments.values()) {
            streetCount.put(segment, segment.getCurrentOccupancy());
        }

        return streetCount;
    }
}
