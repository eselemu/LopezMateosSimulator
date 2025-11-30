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
        // Create a simple 10x1 linear map
        this.trafficMap = new TrafficMap(10, 1);
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
        // Map is already initialized in constructor
        System.out.println("Simple linear map initialized with " +
                trafficMap.getNodes().size() + " nodes");
    }

    /**
     * Calculate a simple route from start to end node (linear movement)
     */
    public Queue<TrafficNode> calculateSimpleRoute(TrafficNode startNode, TrafficNode endNode) {
        Queue<TrafficNode> route = new LinkedList<>();

        // For linear map, just follow the nodes in order
        List<TrafficNode> allNodes = new ArrayList<>(trafficMap.getNodes().values());

        // Sort nodes by x position to ensure correct order
        allNodes.sort(Comparator.comparingInt(node -> node.position.x));

        boolean startFound = false;
        for (TrafficNode node : allNodes) {
            if (node.equals(startNode)) {
                startFound = true;
            }
            if (startFound) {
                route.offer(node);
                if (node.equals(endNode)) {
                    break;
                }
            }
        }

        return route;
    }

    /**
     * Get the next node in the route for a vehicle
     */
    public TrafficNode getNextNode(TrafficNode currentNode, TrafficNode destinationNode) {
        // For linear map, just get the node with next higher x coordinate
        List<TrafficNode> allNodes = new ArrayList<>(trafficMap.getNodes().values());
        allNodes.sort(Comparator.comparingInt(node -> node.position.x));

        int currentIndex = -1;
        for (int i = 0; i < allNodes.size(); i++) {
            if (allNodes.get(i).equals(currentNode)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1 && currentIndex < allNodes.size() - 1) {
            return allNodes.get(currentIndex + 1);
        }

        return null;
    }

    /**
     * Find a node at a specific position
     */
    public TrafficNode getNodeAtPosition(Position position) {
        return trafficMap.getNodeAt(position);
    }

    /**
     * Get start and end nodes for vehicle creation
     */
    public TrafficNode getStartNode() {
        return trafficMap.getNodeById("N_0_0");
    }

    public TrafficNode getEndNode(int maxX) {
        return trafficMap.getNodeById("N_" + maxX + "_0");
    }

    public TrafficNode getTrafficNode(Position from, Position to) {
        // This method is now deprecated - use node-based routing instead
        return getNodeAtPosition(to);
    }

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