package simulation.agents;

import simulation.map.MapManager;
import simulation.map.Position;

import java.util.List;
import java.util.Random;

public class Pedestrian extends Agent {
    private Position currentPosition;
    private MapManager mapManager;
    private SemaphoreSimulation currentSemaphore;
    private Random random;
    private int crossingProgress; // Track crossing progress 0-100%

    public enum PedestrianState {
        WAITING_SEMAPHORE,
        CROSSING,
        FINISHED
    }

    private PedestrianState pedestrianState;

    public Pedestrian(int id) {
        this.id = id;
        this.type = AgentType.PEDESTRIAN;
        this.state = AgentState.ACTIVE;
        this.pedestrianState = PedestrianState.WAITING_SEMAPHORE;
        this.mapManager = MapManager.getInstance();
        this.random = new Random();
        this.crossingProgress = 0;

        initializeAtRandomSemaphore();
    }

    private void initializeAtRandomSemaphore() {
        List<SemaphoreSimulation> allSemaphores = mapManager.getAllSemaphores();
        if (!allSemaphores.isEmpty()) {
            this.currentSemaphore = allSemaphores.get(random.nextInt(allSemaphores.size()));
            // Start on the sidewalk at the semaphore position (UI will handle offset)
            this.currentPosition = new Position(
                    currentSemaphore.getPosition().x,
                    currentSemaphore.getPosition().y
            );
            System.out.println("Peatón " + id + " apareció en semáforo " + currentSemaphore.id);
        } else {
            System.out.println("Peatón " + id + " no pudo encontrar semáforos disponibles");
            pedestrianState = PedestrianState.FINISHED;
            state = AgentState.FINISHED;
        }
    }

    @Override
    public void run() {
        System.out.println("Peatón " + id + " iniciado");

        while (running && pedestrianState != PedestrianState.FINISHED) {
            try {
                switch (pedestrianState) {
                    case WAITING_SEMAPHORE:
                        handleWaitingSemaphore();
                        break;
                    case CROSSING:
                        handleCrossing();
                        break;
                    case FINISHED:
                        return; // Exit thread
                }

                Thread.sleep(100);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Peatón " + id + " finalizado");
    }

    private void handleWaitingSemaphore() throws InterruptedException {
        if (currentSemaphore == null) {
            pedestrianState = PedestrianState.FINISHED;
            return;
        }

        // Try to cross when semaphore is red
        boolean canCross = currentSemaphore.waitForRedLightAndCross();

        if (canCross) {
            pedestrianState = PedestrianState.CROSSING;
            crossingProgress = 0;
            System.out.println("Peatón " + id + " comenzó a cruzar");
        } else {
            // Couldn't cross, wait and try again
            Thread.sleep(500);
        }
    }

    private void handleCrossing() throws InterruptedException {
        if (currentSemaphore == null) {
            pedestrianState = PedestrianState.FINISHED;
            return;
        }

        // Simple crossing: just wait for the crossing time
        long crossingTime = currentSemaphore.getCrossingTime();
        System.out.println("Peatón " + id + " cruzando por " + (crossingTime/1000) + " segundos");

        long startTime = System.currentTimeMillis();

        while (running && pedestrianState == PedestrianState.CROSSING) {
            long elapsed = System.currentTimeMillis() - startTime;
            crossingProgress = (int) ((elapsed * 100) / crossingTime);

            // Check if crossing is complete
            if (elapsed >= crossingTime) {
                // Successfully crossed
                currentSemaphore.finishCrossing();
                pedestrianState = PedestrianState.FINISHED;
                state = AgentState.FINISHED;
                System.out.println("Peatón " + id + " cruzó exitosamente");
                break;
            }

            // Check if semaphore changed to green (should stop crossing)
            if (currentSemaphore.getCurrentState() != SemaphoreSimulation.LightState.RED) {
                System.out.println("Peatón " + id + " detenido - semáforo cambió a verde");
                currentSemaphore.finishCrossing();
                pedestrianState = PedestrianState.WAITING_SEMAPHORE;
                break;
            }

            Thread.sleep(200);
        }
    }

    public void stopPedestrian() {
        if (currentSemaphore != null && pedestrianState == PedestrianState.CROSSING) {
            currentSemaphore.finishCrossing();
        }
        stopAgent();
    }

    // Getters for UI
    public Position getCurrentPosition() { return currentPosition; }
    public PedestrianState getPedestrianState() { return pedestrianState; }
    public SemaphoreSimulation getCurrentSemaphore() { return currentSemaphore; }
    public int getCrossingProgress() { return crossingProgress; }
}