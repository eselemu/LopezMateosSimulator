package simulation.map;

import simulation.agents.Car;
import simulation.agents.SemaphoreSimulation;
import simulation.agents.Truck;

import java.util.*;

public class MapManager {
    private static MapManager instance;
    private TrafficMap trafficMap;
    private PositionRegistry positionRegistry;
    private Map<Position, SemaphoreSimulation> semaphorePositions;
    private List<SemaphoreSimulation> allSemaphores;

    private MapManager() {
        // Create a 2D grid map (e.g., 5x5 intersections = 11x11 nodes)
        this.trafficMap = new TrafficMap(11, 11);
        semaphorePositions = new HashMap<>();
        positionRegistry = new PositionRegistry();
        allSemaphores = new ArrayList<>();
        collectAllSemaphores();
    }

    private void collectAllSemaphores() {
        for (TrafficNode node : trafficMap.getNodes().values()) {
            if (node.getSemaphore() != null) {
                allSemaphores.add(node.getSemaphore());
                registerSemaphore(node.getSemaphore());
            }
        }
    }

    public static MapManager getInstance() {
        if (instance == null) instance = new MapManager();
        return instance;
    }

    public void initializeSimpleMap() {
        System.out.println("2D Grid map initialized with " +
                trafficMap.getNodes().size() + " nodes and " +
                trafficMap.getEdges().size() + " directed edges");
    }

    /**
     * Calculate route using Dijkstra's algorithm
     */
    public Queue<TrafficNode> calculateRoute(TrafficNode startNode, TrafficNode endNode) {
        List<TrafficNode> path = trafficMap.findShortestPath(startNode, endNode);
        Queue<TrafficNode> route = new LinkedList<>(path);

        // Remove the start node if it's in the route (we're already there)
        if (!route.isEmpty() && route.peek().equals(startNode)) {
            route.poll();
        }

        return route;
    }

    /**
     * Get random valid start and end positions for vehicles
     */
    public TrafficNode getRandomStartNode() {
        return trafficMap.getRandomValidStartNode();
    }

    public TrafficNode getRandomEndNode(TrafficNode startNode) {
        return trafficMap.getRandomValidEndNode(startNode);
    }

    /**
     * Find a node at a specific position
     */
    public TrafficNode getNodeAtPosition(Position position) {
        return trafficMap.getNodeAt(position);
    }

    // ... (keep all other existing methods the same)

    public void moveCar(Car car, Position from, Position to) {
        positionRegistry.moveAgent("car_" + car.id, to);
    }

    public void moveTruck(Truck truck, Position oldRear, Position newFront, Position newRear) {
        positionRegistry.removeAgent("truck_" + truck.id + "_front");
        positionRegistry.removeAgent("truck_" + truck.id + "_rear");
        positionRegistry.registerPosition("truck_" + truck.id + "_front", newFront);
        positionRegistry.registerPosition("truck_" + truck.id + "_rear", newRear);
    }

    public SemaphoreSimulation getSemaphoreAt(Position position) {
        return semaphorePositions.get(position);
    }

    public void registerSemaphore(SemaphoreSimulation semaphore) {
        semaphorePositions.put(semaphore.getPosition(), semaphore);
    }

    public List<SemaphoreSimulation> getAllSemaphores() {
        return allSemaphores;
    }

    public Map<TrafficNode, Integer> getTrafficNodes() {
        Map<TrafficNode, Integer> nodeOccupancy = new HashMap<>();
        for (TrafficNode node : trafficMap.getNodes().values()) {
            nodeOccupancy.put(node, node.getCurrentOccupancy());
        }
        return nodeOccupancy;
    }

    public TrafficMap getTrafficMap() {
        return trafficMap;
    }
}