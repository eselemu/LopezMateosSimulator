package simulation.agents;

import simulation.distributed.DistributedSemaphoreClient;
import simulation.distributed.LightStateDTO;
import simulation.map.MapManager;
import simulation.map.Position;
import simulation.map.TrafficNode;
import simulation.TrafficSimulationCore;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Car extends Agent {
    private Position currentPosition;
    private TrafficNode currentNode;
    private TrafficNode destinationNode;
    private Queue<TrafficNode> nodeRoute;
    private MapManager mapManager;
    private DistributedSemaphoreClient distributedClient;
    private TrafficSimulationCore simulationCore;

    public enum CarState {
        MOVING,
        WAITING,
        WAITING_SEMAPHORE,
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
        this.distributedClient = DistributedSemaphoreClient.getInstance();
        this.simulationCore = TrafficSimulationCore.getInstance();

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
                        Thread.sleep(TrafficSimulationCore.vehicleSpeed);

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

    /**
     * Check traffic light at current position.
     * Uses distributed semaphore if distributed mode is enabled, otherwise uses local semaphore.
     */
    private void checkTrafficLight(Position position) {
        SemaphoreSimulation semaphore = mapManager.getSemaphoreAt(position);
        if (semaphore == null) {
            carState = CarState.MOVING;
            return;
        }

        // Check if distributed mode is enabled
        boolean useDistributed = simulationCore.isDistributedModeEnabled();
        
        if (useDistributed) {
            // Use distributed semaphore client
            checkTrafficLightDistributed(semaphore.id);
        } else {
            // Use local semaphore
            checkTrafficLightLocal(semaphore);
        }
    }

    /**
     * Check traffic light using distributed semaphore client
     */
    private void checkTrafficLightDistributed(int semaphoreId) {
        LightStateDTO stateDTO = distributedClient.getCurrentState(semaphoreId);
        if (stateDTO == null) {
            // Fallback to local if distributed fails
            SemaphoreSimulation semaphore = mapManager.getSemaphoreAt(currentPosition);
            if (semaphore != null) {
                checkTrafficLightLocal(semaphore);
            }
            return;
        }

        // Check current state
        if (stateDTO.currentState == LightStateDTO.State.YELLOW || 
            stateDTO.currentState == LightStateDTO.State.RED) {
            carState = CarState.WAITING_SEMAPHORE;
            System.out.println("Car " + id + " detected " + stateDTO.currentState + 
                           " light at distributed semaphore " + semaphoreId + ", waiting...");

            // Request green light and wait
            boolean canProceed = false;
            while (running && !canProceed) {
                canProceed = distributedClient.requestGreenLight(semaphoreId, id);
                if (!canProceed) {
                    try {
                        Thread.sleep(100); // Wait before retrying
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (canProceed) {
                carState = CarState.MOVING;
                System.out.println("Car " + id + " can proceed - green light from distributed semaphore " + semaphoreId);
            }
        } else {
            carState = CarState.MOVING;
            System.out.println("Car " + id + " green light from distributed semaphore " + semaphoreId + ", proceeding...");
        }
    }

    /**
     * Check traffic light using local semaphore
     */
    private void checkTrafficLightLocal(SemaphoreSimulation semaphore) {
        if (semaphore.getCurrentState() == SemaphoreSimulation.LightState.YELLOW || 
            semaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) {
            carState = CarState.WAITING_SEMAPHORE;
            System.out.println("Car " + id + " detected red light at position " + currentPosition + ", waiting...");

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