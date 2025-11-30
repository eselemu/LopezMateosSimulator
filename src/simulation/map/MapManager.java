package simulation.map;

import simulation.agents.Car;
import simulation.agents.SemaphoreSimulation;
import simulation.agents.Truck;

import java.util.HashMap;
import java.util.Map;

public class MapManager {
    private static MapManager instance;
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
        // Create street segments
        for(int i = 0; i < 10; i++) {
            String segmentId = "street_" + i + "_" + (i+1);
            StreetSegment segment = new StreetSegment(segmentId, 1);
            streetSegments.put(segmentId, segment);
        }
    }

    public StreetSegment getStreetSegment(Position from, Position to) {
        String segmentId = "street_" + from.x + "_" + to.x;
        return streetSegments.get(segmentId);
    }

    public void moveCar(Car car, Position from, Position to) {
        positionRegistry.moveAgent("car_" + car.id, to);
    }

    // Updated method for moving trucks
    public void moveTruck(Truck truck, Position oldRear, Position newFront, Position newRear) {
        // Remove old positions
        positionRegistry.removeAgent("truck_" + truck.id + "_front");
        positionRegistry.removeAgent("truck_" + truck.id + "_rear");

        // Register new positions
        positionRegistry.registerPosition("truck_" + truck.id + "_front", newFront);
        positionRegistry.registerPosition("truck_" + truck.id + "_rear", newRear);
    }

    public SemaphoreSimulation getSemaphoreAt(Position position) {
        return semaphorePositions.get(position);
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