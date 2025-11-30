package simulation.agents;

import simulation.map.MapManager;
import simulation.map.Position;
import simulation.map.StreetSegment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Truck extends Agent {
    private Position currentPosition;  // Front position of the truck
    private Position rearPosition;     // Rear position of the truck
    private Position destination;
    private BlockingQueue<Position> route;
    private MapManager mapManager;
    private StreetSegment currentFrontSegment; // Segment for front position
    private StreetSegment currentRearSegment;  // Segment for rear position

    public enum TruckState {
        MOVING,
        WAITING,
        WAITING_SEMAPHORE,
        WAITING_DOUBLE_SEGMENT,
        IN_INTERSECTION,
        FINISHED
    }

    private TruckState truckState;

    public Truck(int id, Position start, Position destination) {
        this.id = id;
        // Initialize rear position as one position behind the start
        this.rearPosition = new Position(start.x - 1, start.y);
        this.currentPosition = start;
        this.destination = destination;
        this.type = AgentType.TRUCK;
        this.state = AgentState.ACTIVE;
        this.truckState = TruckState.MOVING;
        this.mapManager = MapManager.getInstance();
        this.route = new LinkedBlockingQueue<>();

        calculateSimpleRoute();
    }

    private void calculateSimpleRoute() {
        for(int x = currentPosition.x + 1; x <= destination.x; x++) {
            route.offer(new Position(x, currentPosition.y));
        }
    }


    @Override
    public void run() {
        acquireInitialSegments();
        System.out.println("Truck " + id + " iniciado - Frente: " + currentPosition + " - Trasero: " + rearPosition);

        boolean firstMove = true;

        while (running && !route.isEmpty()) {
            try {
                Position nextPosition = route.peek();

                if(nextPosition != null) {
                    StreetSegment frontSegment = mapManager.getStreetSegment(currentPosition, nextPosition);

                    if(frontSegment != null) {
                        boolean canMove = false;

                        if (firstMove) {
                            canMove = (currentFrontSegment != null);
                            firstMove = false;
                        } else {
                            // Subsequent moves: try to acquire new front segment
                            canMove = tryAcquireDoubleSegment(frontSegment, nextPosition);
                        }

                        if(canMove) {
                            route.poll();

                            // Release the OLD rear segment
                            if (currentRearSegment != null) {
                                currentRearSegment.release();
                                System.out.println("Truck " + id + " released OLD rear segment: " + currentRearSegment.getSegmentId());
                            }

                            // Update positions
                            rearPosition = currentPosition;
                            currentPosition = nextPosition;

                            currentRearSegment = currentFrontSegment;
                            currentFrontSegment = frontSegment;  // Always update (first move: same reference, other moves: new segment)

                            mapManager.moveTruck(this, rearPosition, currentPosition, rearPosition);

                            System.out.println("Truck " + id + " movido - Frente: " + currentPosition +
                                    " - Trasero: " + rearPosition +
                                    " - Front Segment: " + (currentFrontSegment != null ? currentFrontSegment.getSegmentId() : "null") +
                                    " - Rear Segment: " + (currentRearSegment != null ? currentRearSegment.getSegmentId() : "null") +
                                    " - Estado: " + truckState);

                            checkTrafficLightForTruck();
                            Thread.sleep(1000);
                        } else {
                            truckState = TruckState.WAITING_DOUBLE_SEGMENT;
                            System.out.println("Truck " + id + " esperando - no puede adquirir segmento frontal");
                            Thread.sleep(500);
                        }
                    }
                }

                if(currentPosition.equals(destination)) {
                    releasePreviousSegments();
                    truckState = TruckState.FINISHED;
                    state = AgentState.FINISHED;
                    System.out.println("Truck " + id + " llegó a su destino");
                    break;
                }

            } catch (InterruptedException e) {
                releasePreviousSegments();
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean tryAcquireDoubleSegment(StreetSegment frontSegment, Position nextPosition) {
        // First check semaphores at both positions the truck will occupy after movement
        SemaphoreSimulation newFrontSemaphore = mapManager.getSemaphoreAt(nextPosition);
        SemaphoreSimulation newRearSemaphore = mapManager.getSemaphoreAt(currentPosition);

        // If there's a red light at either position, we need to wait
        if ((newFrontSemaphore != null && newFrontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) ||
                (newRearSemaphore != null && newRearSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED)) {

            truckState = TruckState.WAITING_SEMAPHORE;
            System.out.println("Truck " + id + " detectó semáforo rojo, esperando...");
            return false;
        }

        // Try to acquire the NEW front segment (currentPosition → nextPosition)
        if (frontSegment.tryAcquire()) {
            System.out.println("Truck " + id + " adquirió segmento frontal: " + frontSegment.getSegmentId());
            return true;
        } else {
            System.out.println("Truck " + id + " no pudo adquirir segmento frontal: " + frontSegment.getSegmentId());
            return false;
        }
    }

    private void acquireInitialSegments() {
        // Acquire BOTH initial segments
        StreetSegment rearSeg = mapManager.getStreetSegment(rearPosition, currentPosition);

        if (rearSeg != null && rearSeg.tryAcquire()) {
            currentRearSegment = rearSeg;
            System.out.println("Truck " + id + " adquirió segmento trasero inicial: " + rearSeg.getSegmentId());
        }

        // ALSO acquire the front segment (from current to first destination)
        Position firstDest = route.peek();
        if (firstDest != null) {
            StreetSegment frontSeg = mapManager.getStreetSegment(currentPosition, firstDest);
            if (frontSeg != null && frontSeg.tryAcquire()) {
                currentFrontSegment = frontSeg;
                System.out.println("Truck " + id + " adquirió segmento frontal inicial: " + frontSeg.getSegmentId());
            }
        }
    }

    /**
     * Release both segments the truck currently occupies
     */
    /**
     * Release both segments the truck currently occupies
     */
    private void releasePreviousSegments() {
        // Release in reverse order: front first, then rear
        if (currentFrontSegment != null) {
            currentFrontSegment.release();
            System.out.println("Truck " + id + " released front segment: " + currentFrontSegment.getSegmentId());
            currentFrontSegment = null;
        }
        if (currentRearSegment != null) {
            currentRearSegment.release();
            System.out.println("Truck " + id + " released rear segment: " + currentRearSegment.getSegmentId());
            currentRearSegment = null;
        }
    }

    /**
     * Check traffic light for truck considering it occupies 2 positions
     */
    private void checkTrafficLightForTruck() {
        // Check both positions the truck occupies
        SemaphoreSimulation frontSemaphore = mapManager.getSemaphoreAt(currentPosition);
        SemaphoreSimulation rearSemaphore = mapManager.getSemaphoreAt(rearPosition);

        // If either semaphore is red, wait for both to be green
        if ((frontSemaphore != null && frontSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) ||
                (rearSemaphore != null && rearSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED)) {

            truckState = TruckState.WAITING_SEMAPHORE;
            System.out.println("Truck " + id + " detectó semáforo rojo en posición " +
                    currentPosition + " o " + rearPosition + ", esperando...");

            try {
                // Wait for both semaphores to be green
                waitForBothSemaphoresGreen(frontSemaphore, rearSemaphore);

                truckState = TruckState.MOVING;
                System.out.println("Truck " + id + " puede avanzar - ambos semáforos verdes!");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Truck " + id + " interrumpido mientras esperaba semáforo");
            }
        } else {
            truckState = TruckState.MOVING;
            System.out.println("Truck " + id + " ambos semáforos verdes, avanzando...");
        }
    }

    /**
     * Wait until both semaphores are green
     */
    private void waitForBothSemaphoresGreen(SemaphoreSimulation frontSem, SemaphoreSimulation rearSem)
            throws InterruptedException {
        while (running &&
                ((frontSem != null && frontSem.getCurrentState() != SemaphoreSimulation.LightState.GREEN) ||
                        (rearSem != null && rearSem.getCurrentState() != SemaphoreSimulation.LightState.GREEN))) {

            // Wait a bit and check again
            Thread.sleep(100);
        }
    }

    public void stopTruck() {
        releasePreviousSegments();
        stopAgent();
    }

    // Getters para UI
    public Position getCurrentPosition() { return currentPosition; }
    public Position getRearPosition() { return rearPosition; } // NEW: Get rear position
    public TruckState getTruckState() { return truckState; }
    public Position getDestination() { return destination; }

    // Method to get both positions occupied by the truck
    public Position[] getOccupiedPositions() {
        return new Position[] {
                currentPosition,
                rearPosition
        };
    }
}