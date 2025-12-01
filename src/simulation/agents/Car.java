package simulation.agents;

import simulation.map.MapManager;
import simulation.map.Position;
import simulation.map.TrafficNode;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Car extends Agent {
    private Position currentPosition;
    private TrafficNode currentNode;
    private TrafficNode destinationNode;
    private Queue<TrafficNode> nodeRoute;
    private MapManager mapManager;

    public enum CarState {
        MOVING,
        WAITING,
        WAITING_SEMAPHORE,
        IN_INTERSECTION,
        FINISHED
    }

    private CarState carState;

    public Car(int id, Position start, Position destination) {
        this.id = id;
        this.currentPosition = start;
        this.type = AgentType.CAR;
        this.state = AgentState.ACTIVE;
        this.carState = CarState.MOVING;
        this.mapManager = MapManager.getInstance();
        this.nodeRoute = new LinkedBlockingQueue<>();

        // Convert positions to nodes and calculate route
        this.currentNode = mapManager.getNodeAtPosition(start);
        this.destinationNode = mapManager.getNodeAtPosition(destination);
        calculateNodeRoute();
    }

    private void calculateNodeRoute() {
        if (currentNode != null && destinationNode != null) {
            // Use the new dynamic route calculation
            nodeRoute = mapManager.calculateRoute(currentNode, destinationNode);
            System.out.println("Car " + id + " route calculated: " + nodeRoute.size() + " nodes");
        } else {
            System.out.println("Car " + id + " could not find start or end node!");
            carState = CarState.FINISHED;
            state = AgentState.FINISHED;
        }
    }

    @Override
    public void run() {
        System.out.println("Car " + id + " started at node: " +
                (currentNode != null ? currentNode.nodeId : "null"));

        while (running && !nodeRoute.isEmpty()) {
            try {
                TrafficNode nextNode = nodeRoute.peek();

                if (nextNode != null) {
                    // Try to acquire the next node
                    if (nextNode.tryAcquire(this)) {
                        // Successfully acquired the node - move to it
                        nodeRoute.poll(); // Remove from queue

                        // Release current node if exists
                        if (currentNode != null) {
                            currentNode.release();
                            System.out.println("Car " + id + " released node: " + currentNode.nodeId);
                        }

                        // Update position and current node
                        Position oldPosition = currentPosition;
                        currentPosition = nextNode.position;
                        currentNode = nextNode;

                        mapManager.moveCar(this, oldPosition, currentPosition);

                        System.out.println("Car " + id + " moved to: " + currentNode.nodeId +
                                " - Position: " + currentPosition + " - State: " + carState);

                        // Check traffic light at new position
                        checkTrafficLight(currentPosition);

                        // Simulate time spent at this node
                        Thread.sleep(1000);

                    } else {
                        // Node is occupied, wait
                        carState = CarState.WAITING;
                        System.out.println("Car " + id + " waiting for node: " + nextNode.nodeId);
                        //Thread.sleep(50);
                    }
                }

                // Check if reached destination
                if (currentNode != null && currentNode.equals(destinationNode)) {
                    carState = CarState.FINISHED;
                    state = AgentState.FINISHED;
                    System.out.println("Car " + id + " reached destination node: " + currentNode.nodeId);
                    break;
                }

            } catch (InterruptedException e) {
                // Release current node if interrupted
                if (currentNode != null) {
                    currentNode.release();
                }
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Release final node when finished
        if (currentNode != null) {
            currentNode.release();
        }
    }

    private void checkTrafficLight(Position position) {
        SemaphoreSimulation semaphore = mapManager.getSemaphoreAt(position);
        if (semaphore != null) {
            if (semaphore.getCurrentState() == SemaphoreSimulation.LightState.YELLOW || semaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) {
                carState = CarState.WAITING_SEMAPHORE;
                System.out.println("Car " + id + " detected red light at position " + position + ", waiting...");

                try {
                    semaphore.waitForGreenLight();
                    carState = CarState.MOVING;
                    System.out.println("Car " + id + " can proceed - green light!");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Car " + id + " interrupted while waiting for semaphore");
                }
            } else {
                carState = CarState.MOVING;
                System.out.println("Car " + id + " green light, proceeding...");
            }
        } else {
            carState = CarState.MOVING;
        }
    }

    public void stopCar() {
        if (currentNode != null) {
            currentNode.release();
        }
        stopAgent();
    }

    // Getters for UI
    public Position getCurrentPosition() { return currentPosition; }
    public CarState getCarState() { return carState; }
    public Position getDestination() {
        return destinationNode != null ? destinationNode.position : new Position(0, 0);
    }
    public Queue<TrafficNode> getNodeRoute() { return nodeRoute; }
}