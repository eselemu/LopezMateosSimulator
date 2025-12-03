package simulation.agents;

import simulation.distributed.DistributedSemaphoreClient;
import simulation.distributed.LightStateDTO;
import simulation.map.MapManager;
import simulation.map.Position;
import simulation.TrafficSimulationCore;

import java.util.List;
import java.util.Random;

public class Pedestrian extends Agent {
    private Position currentPosition;
    private MapManager mapManager;
    private SemaphoreSimulation currentSemaphore;
    private Random random;
    private int crossingProgress; // Track crossing progress 0-100%
    private DistributedSemaphoreClient distributedClient;
    private TrafficSimulationCore simulationCore;

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
        this.distributedClient = DistributedSemaphoreClient.getInstance();
        this.simulationCore = TrafficSimulationCore.getInstance();

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

    /**
     * Handle waiting for semaphore to allow crossing.
     * Uses distributed semaphore if distributed mode is enabled, otherwise uses local semaphore.
     */
    private void handleWaitingSemaphore() throws InterruptedException {
        if (currentSemaphore == null) {
            pedestrianState = PedestrianState.FINISHED;
            return;
        }

        // Check if distributed mode is enabled
        boolean useDistributed = simulationCore.isDistributedModeEnabled();
        
        boolean canCross;
        if (useDistributed) {
            // Use distributed semaphore client
            canCross = handleWaitingSemaphoreDistributed();
        } else {
            // Use local semaphore
            canCross = currentSemaphore.waitForRedLightAndCross();
        }

        if (canCross) {
            pedestrianState = PedestrianState.CROSSING;
            crossingProgress = 0;
            System.out.println("Peatón " + id + " comenzó a cruzar");
        } else {
            // Couldn't cross, wait and try again
            Thread.sleep(500);
        }
    }

    /**
     * Handle waiting for semaphore using distributed client
     */
    private boolean handleWaitingSemaphoreDistributed() {
        if (currentSemaphore == null) {
            return false;
        }

        int semaphoreId = currentSemaphore.id;
        
        // Check if semaphore is red
        LightStateDTO stateDTO = distributedClient.getCurrentState(semaphoreId);
        if (stateDTO == null || stateDTO.currentState != LightStateDTO.State.RED) {
            // Fallback to local if distributed fails or not red
            try {
                return currentSemaphore.waitForRedLightAndCross();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        // Request crossing permission
        boolean canCross = distributedClient.requestCrossing(semaphoreId, id);
        
        if (canCross) {
            System.out.println("Pedestrian " + id + " granted crossing permission from distributed semaphore " + semaphoreId);
        } else {
            System.out.println("Pedestrian " + id + " denied crossing permission from distributed semaphore " + semaphoreId);
        }
        
        return canCross;
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
                finishCrossingAtSemaphore();
                pedestrianState = PedestrianState.FINISHED;
                state = AgentState.FINISHED;
                System.out.println("Peatón " + id + " cruzó exitosamente");
                break;
            }

            // Check if semaphore changed to green (should stop crossing)
            boolean shouldStop = checkSemaphoreStateChanged();
            if (shouldStop) {
                System.out.println("Peatón " + id + " detenido - semáforo cambió a verde");
                finishCrossingAtSemaphore();
                pedestrianState = PedestrianState.WAITING_SEMAPHORE;
                break;
            }

            Thread.sleep(200);
        }
    }

    /**
     * Finish crossing at semaphore (distributed or local)
     */
    private void finishCrossingAtSemaphore() {
        if (currentSemaphore == null) {
            return;
        }

        boolean useDistributed = simulationCore.isDistributedModeEnabled();
        
        if (useDistributed) {
            distributedClient.finishCrossing(currentSemaphore.id, id);
        } else {
            currentSemaphore.finishCrossing();
        }
    }

    /**
     * Check if semaphore state changed (distributed or local)
     */
    private boolean checkSemaphoreStateChanged() {
        if (currentSemaphore == null) {
            return true;
        }

        boolean useDistributed = simulationCore.isDistributedModeEnabled();
        
        if (useDistributed) {
            LightStateDTO stateDTO = distributedClient.getCurrentState(currentSemaphore.id);
            if (stateDTO == null) {
                // Fallback to local check
                return currentSemaphore.getCurrentState() != SemaphoreSimulation.LightState.RED;
            }
            return stateDTO.currentState != LightStateDTO.State.RED;
        } else {
            return currentSemaphore.getCurrentState() != SemaphoreSimulation.LightState.RED;
        }
    }

    public void stopPedestrian() {
        if (currentSemaphore != null && pedestrianState == PedestrianState.CROSSING) {
            finishCrossingAtSemaphore();
        }
        stopAgent();
    }

    // Getters for UI
    public Position getCurrentPosition() { return currentPosition; }
    public PedestrianState getPedestrianState() { return pedestrianState; }
    public SemaphoreSimulation getCurrentSemaphore() { return currentSemaphore; }
    public int getCrossingProgress() { return crossingProgress; }
}