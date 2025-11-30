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
        WAITING_DOUBLE_NODE,
        FINISHED
    }

    private TruckState truckState;

    public Truck(int id, Position start, Position destination) {
        this.id = id;
        this.currentPosition = start;
        this.rearPosition = new Position(start.x - 1, start.y);
        this.type = AgentType.TRUCK;
        this.state = AgentState.ACTIVE;
        this.truckState = TruckState.MOVING;
        this.mapManager = MapManager.getInstance();
        this.nodeRoute = new LinkedBlockingQueue<>();

        // Convert positions to nodes
        this.currentFrontNode = mapManager.getNodeAtPosition(start);
        this.currentRearNode = mapManager.getNodeAtPosition(rearPosition);
        this.destinationNode = mapManager.getNodeAtPosition(destination);

        calculateNodeRoute();
        acquireInitialNodes();
    }

    private void calculateNodeRoute() {
        if (currentFrontNode != null && destinationNode != null) {
            nodeRoute = mapManager.calculateSimpleRoute(currentFrontNode, destinationNode);
            // Remove the current front node from the route (we're already there)
            if (!nodeRoute.isEmpty()) {
                nodeRoute.poll();
            }
            System.out.println("Truck " + id + " route calculated: " + nodeRoute.size() + " nodes");
        }
    }

    private void acquireInitialNodes() {
        if (currentFrontNode != null && currentFrontNode.tryAcquire(this)) {
            System.out.println("Truck " + id + " acquired front node: " + currentFrontNode.nodeId);
        }
        if (currentRearNode != null && currentRearNode.tryAcquire(this)) {
            System.out.println("Truck " + id + " acquired rear node: " + currentRearNode.nodeId);
        }
    }

    @Override
    public void run() {
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
                        truckState = TruckState.WAITING_DOUBLE_NODE;
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
        // Check semaphores first
        /*SemaphoreSimulation frontSemaphore = mapManager.getSemaphoreAt(nextNode.position);
        SemaphoreSimulation rearSemaphore = mapManager.getSemaphoreAt(currentPosition);

        if ((frontSemaphore != null && frontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) ||
                (rearSemaphore != null && rearSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED)) {

            truckState = TruckState.WAITING_SEMAPHORE;
            System.out.println("Truck " + id + " waiting for semaphore");
            return false;
        }*/

        // Try to acquire the next node
        return nextNode.tryAcquire(this);
    }

    private void checkTrafficLightForTruck() {
        SemaphoreSimulation frontSemaphore = mapManager.getSemaphoreAt(currentPosition);
        SemaphoreSimulation rearSemaphore = mapManager.getSemaphoreAt(rearPosition);

        if ((frontSemaphore != null && (frontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.YELLOW || frontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED)) /*||
                (rearSemaphore != null && (rearSemaphore.getCurrentState() == SemaphoreSimulation.LightState.YELLOW || rearSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED))*/) {

            truckState = TruckState.WAITING_SEMAPHORE;
            System.out.println("Truck " + id + " detected red light, waiting...");

            try {
                waitForBothSemaphoresGreen(frontSemaphore, rearSemaphore);
                truckState = TruckState.MOVING;
                System.out.println("Truck " + id + " can proceed - both semaphores green!");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            truckState = TruckState.MOVING;
        }
    }

    private void waitForBothSemaphoresGreen(SemaphoreSimulation frontSem, SemaphoreSimulation rearSem)
            throws InterruptedException {
        while (running &&
                ((frontSem != null && frontSem.getCurrentState() != SemaphoreSimulation.LightState.GREEN) ||
                        (rearSem != null && rearSem.getCurrentState() != SemaphoreSimulation.LightState.GREEN))) {
            Thread.sleep(100);
        }
    }

    private void releaseAllNodes() {
        if (currentFrontNode != null) {
            currentFrontNode.release();
            currentFrontNode = null;
        }
        if (currentRearNode != null) {
            currentRearNode.release();
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