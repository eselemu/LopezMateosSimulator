package simulation.agents;

import simulation.map.MapManager;
import simulation.map.Position;
import simulation.map.TrafficNode;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Truck extends Agent {
    private Position currentPosition;
    private Position rearPosition;
    private TrafficNode currentFrontNode;
    private TrafficNode currentRearNode;
    private TrafficNode destinationNode;
    private Queue<TrafficNode> nodeRoute;
    private MapManager mapManager;

    public enum TruckState {
        MOVING,
        WAITING,
        WAITING_SEMAPHORE,
        FINISHED
    }

    private TruckState truckState;

    public Truck(int id, Position start, Position destination) {
        this.id = id;
        this.currentPosition = start;
        this.type = AgentType.TRUCK;
        this.state = AgentState.ACTIVE;
        this.truckState = TruckState.MOVING;
        this.mapManager = MapManager.getInstance();
        this.nodeRoute = new LinkedBlockingQueue<>();

        // Convert positions to nodes
        this.currentFrontNode = mapManager.getNodeAtPosition(start);
        this.destinationNode = mapManager.getNodeAtPosition(destination);

    }

    private void calculateNodeRoute() {
        if (currentFrontNode != null && destinationNode != null) {
            // Use the new dynamic route calculation
            nodeRoute = mapManager.calculateRoute(currentFrontNode, destinationNode);
            System.out.println("Truck " + id + " route calculated: " + nodeRoute.size() + " nodes");
        } else {
            System.out.println("Truck " + id + " could not find start or end node!");
            truckState = TruckState.FINISHED;
            state = AgentState.FINISHED;
        }
    }

    /**
     * Calculate the initial rear position based on the truck's intended direction
     */
    private void calculateInitialRearPosition() {
        if (nodeRoute.isEmpty()) {
            // If no route, default to behind the start position (backup)
            this.rearPosition = new Position(currentPosition.x - 1, currentPosition.y);
            this.currentRearNode = mapManager.getNodeAtPosition(rearPosition);
            return;
        }

        // Get the first node we're moving to
        TrafficNode firstMoveNode = nodeRoute.peek();

        // Calculate direction from current position to first move
        int dx = firstMoveNode.position.x - currentPosition.x;
        int dy = firstMoveNode.position.y - currentPosition.y;

        // Normalize direction (we're only moving in one direction at a time in grid)
        if (Math.abs(dx) > Math.abs(dy)) {
            // Horizontal movement
            if (dx > 0) {
                // Moving right → rear should be left
                this.rearPosition = new Position(currentPosition.x - 1, currentPosition.y);
            } else {
                // Moving left → rear should be right
                this.rearPosition = new Position(currentPosition.x + 1, currentPosition.y);
            }
        } else {
            // Vertical movement
            if (dy > 0) {
                // Moving down → rear should be up
                this.rearPosition = new Position(currentPosition.x, currentPosition.y - 1);
            } else {
                // Moving up → rear should be down
                this.rearPosition = new Position(currentPosition.x, currentPosition.y + 1);
            }
        }

        this.currentRearNode = mapManager.getNodeAtPosition(rearPosition);
        System.out.println("Truck " + id + " rear position calculated: " + rearPosition +
                " (direction: dx=" + dx + ", dy=" + dy + ")");
    }

    private void acquireInitialNodes() {
        if (currentFrontNode != null && currentFrontNode.tryAcquire(this)) {
            System.out.println("Truck " + id + " acquired front node: " + currentFrontNode.nodeId);
        }
        if (currentRearNode != null && currentRearNode.tryAcquire(this)) {
            System.out.println("Truck " + id + " acquired rear node: " + currentRearNode.nodeId);
        } else {
            System.out.println("Truck " + id + " failed to acquire rear node: " +
                    (currentRearNode != null ? currentRearNode.nodeId : "null"));
        }
    }

    @Override
    public void run() {

        calculateNodeRoute();
        calculateInitialRearPosition();
        acquireInitialNodes();

        System.out.println("Truck " + id + " started - Front: " + currentPosition + " Rear: " + rearPosition);

        while (running && !nodeRoute.isEmpty()) {
            try {
                TrafficNode nextNode = nodeRoute.peek();

                if (nextNode != null) {
                    // Try to acquire both the next node and release the rear node
                    if (tryAcquireNextNode(nextNode)) {
                        nodeRoute.poll(); // Remove from queue

                        // Update positions
                        Position oldRear = rearPosition;
                        rearPosition = currentPosition;
                        currentPosition = nextNode.position;

                        // Update node occupancy
                        if (currentRearNode != null) {
                            currentRearNode.release();
                            System.out.println("Truck " + id + " released rear node: " + currentRearNode.nodeId);
                        }

                        currentRearNode = currentFrontNode;
                        currentFrontNode = nextNode;

                        mapManager.moveTruck(this, oldRear, currentPosition, rearPosition);

                        System.out.println("Truck " + id + " moved - Front: " + currentFrontNode.nodeId +
                                " Rear: " + currentRearNode.nodeId + " - State: " + truckState);

                        checkTrafficLightForTruck();
                        Thread.sleep(1000); // Movement time

                    } else {
                        truckState = TruckState.WAITING;
                        Thread.sleep(200);
                        System.out.println("Truck " + id + " waiting for node access");
                    }
                }

                // Check if reached destination
                if (currentFrontNode != null && currentFrontNode.equals(destinationNode)) {
                    releaseAllNodes();
                    truckState = TruckState.FINISHED;
                    state = AgentState.FINISHED;
                    System.out.println("Truck " + id + " reached destination");
                    break;
                }

            } catch (InterruptedException e) {
                releaseAllNodes();
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean tryAcquireNextNode(TrafficNode nextNode) {
        // Try to acquire the next node
        boolean acquired = nextNode.tryAcquire(this);
        if (!acquired) {
            System.out.println("Truck " + id + " failed to acquire next node: " + nextNode.nodeId);
        }
        return acquired;
    }

    private void checkTrafficLightForTruck() {
        SemaphoreSimulation frontSemaphore = mapManager.getSemaphoreAt(currentPosition);
        SemaphoreSimulation rearSemaphore = mapManager.getSemaphoreAt(rearPosition);

        if ((frontSemaphore != null &&
                (frontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.YELLOW ||
                        frontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED))) {

            truckState = TruckState.WAITING_SEMAPHORE;
            System.out.println("Truck " + id + " detected red light at front, waiting...");

            try {
                waitForGreenLight(frontSemaphore);
                truckState = TruckState.MOVING;
                System.out.println("Truck " + id + " can proceed - semaphore green!");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            truckState = TruckState.MOVING;
        }
    }

    private void waitForGreenLight(SemaphoreSimulation semaphore) throws InterruptedException {
        while (running && semaphore != null &&
                semaphore.getCurrentState() != SemaphoreSimulation.LightState.GREEN) {
            Thread.sleep(100);
        }
    }

    private void releaseAllNodes() {
        if (currentFrontNode != null) {
            currentFrontNode.release();
            System.out.println("Truck " + id + " released front node: " + currentFrontNode.nodeId);
            currentFrontNode = null;
        }
        if (currentRearNode != null) {
            currentRearNode.release();
            System.out.println("Truck " + id + " released rear node: " + currentRearNode.nodeId);
            currentRearNode = null;
        }
    }

    public void stopTruck() {
        releaseAllNodes();
        stopAgent();
    }

    // Getters for UI
    public Position getCurrentPosition() { return currentPosition; }
    public Position getRearPosition() { return rearPosition; }
    public TruckState getTruckState() { return truckState; }
    public Position getDestination() {
        return destinationNode != null ? destinationNode.position : new Position(0, 0);
    }
    public Queue<TrafficNode> getNodeRoute() { return nodeRoute; }
}