package simulation.agents;

import simulation.map.MapManager;
import simulation.map.Position;
import simulation.map.StreetSegment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Car extends Agent {
    private Position currentPosition;
    private Position destination;
    private BlockingQueue<Position> route;
    private MapManager mapManager;
    private StreetSegment currentSegment; // Track current segment

    public enum CarState {
        MOVING,
        WAITING,
        WAITING_SEMAPHORE,
        IN_INTERSECTION,
        FINISHED
    }

    private CarState carState;

    public Car(int id, Position start, Position destination){
        this.id = id;
        this.currentPosition = start;
        this.destination = destination;
        this.type = AgentType.CAR;
        this.state = AgentState.ACTIVE;
        this.carState = CarState.MOVING;
        this.mapManager = MapManager.getInstance();
        this.route = new LinkedBlockingQueue<>();
        calculateSimpleRoute();
    }

    private void calculateSimpleRoute() {
        // Ruta simple: mover de start a destination en línea recta
        for(int x = currentPosition.x + 1; x <= destination.x; x++) {
            route.offer(new Position(x, currentPosition.y));
        }
    }

    @Override
    public void run(){
        System.out.println("Car " + id + " iniciado en posición: " + currentPosition);

        while(running && !route.isEmpty()) {
            try {
                Position nextPosition = route.peek();

                if(nextPosition != null) {
                    StreetSegment segment = mapManager.getStreetSegment(currentPosition, nextPosition);

                    if(segment != null) {
                        // Intentar adquirir el segmento de calle (zona crítica)
                        if(segment.tryAcquire()) {
                            // Mover al siguiente segmento
                            route.poll(); // Remover de la cola

                            // Release previous segment if exists
                            if (currentSegment != null) {
                                currentSegment.release();
                                System.out.println("Car " + id + " released previous segment: " + currentSegment.getSegmentId());
                            }

                            // Update position and current segment
                            mapManager.moveCar(this, currentPosition, nextPosition);
                            currentPosition = nextPosition;
                            currentSegment = segment; // Hold reference to current segment

                            System.out.println("Car " + id + " movido a: " + currentPosition +
                                    " - Segment: " + segment.getSegmentId() +
                                    " - Estado: " + carState);

                            // Verificar semáforo
                            checkTrafficLight(nextPosition);

                            // IMPORTANT: Sleep while HOLDING the lock
                            // This simulates the time spent in this position
                            Thread.sleep(1000);

                            // DON'T release here - we release when moving to next position
                            // segment.release(); // REMOVE THIS LINE

                        } else {
                            // Segment ocupado, esperar
                            carState = CarState.WAITING;
                            System.out.println("Car " + id + " esperando - segmento ocupado: " + segment.getSegmentId());
                            Thread.sleep(500);
                        }
                    }
                }

                if(currentPosition.equals(destination)) {
                    // Release final segment when destination reached
                    if (currentSegment != null) {
                        currentSegment.release();
                        System.out.println("Car " + id + " released final segment: " + currentSegment.getSegmentId());
                    }
                    carState = CarState.FINISHED;
                    state = AgentState.FINISHED;
                    System.out.println("Car " + id + " llegó a su destino");
                    break;
                }

            } catch (InterruptedException e) {
                // Release segment if interrupted
                if (currentSegment != null) {
                    currentSegment.release();
                }
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void checkTrafficLight(Position position) {
        if(position.x == 5) {
            System.out.println("Que rollo");
        }
        SemaphoreSimulation semaphore = mapManager.getSemaphoreAt(position);
        if(semaphore != null && semaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) {
            carState = CarState.WAITING;
            System.out.println("Car " + id + " esperando en semáforo rojo");
            try {
                Thread.sleep(5000); // Espera en semáforo rojo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            carState = CarState.MOVING;
        }
    }

    public void stopCar() {
        // Release current segment when stopping
        if (currentSegment != null) {
            currentSegment.release();
        }
        stopAgent();
    }

    // Getters para UI
    public Position getCurrentPosition() { return currentPosition; }
    public CarState getCarState() { return carState; }
    public Position getDestination() { return destination; }
}
