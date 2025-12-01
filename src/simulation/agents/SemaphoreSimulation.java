package simulation.agents;

import simulation.map.Position;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreSimulation extends Agent {
    private static int redLightTimer;
    private static int yellowLightTimer;
    private static int greenLightTimer;

    public enum LightState { GREEN, YELLOW, RED }

    private volatile LightState currentState;
    private Position position;
    private long lastChangeTime;

    private final ReentrantLock stateLock;
    private final Condition greenLightCondition;
    private final Condition redLightCondition;

    // Pedestrian control - only N pedestrians can cross simultaneously during red light
    private final Semaphore pedestrianCrossingSemaphore;
    private static final int MAX_CROSSING_PEDESTRIANS = 3;

    public SemaphoreSimulation(int id, Position position) {
        this.id = id;
        this.position = position;
        this.type = AgentType.SEMAPHORE;
        this.state = AgentState.ACTIVE;
        this.currentState = LightState.RED; // Start at red
        this.lastChangeTime = System.currentTimeMillis();

        this.stateLock = new ReentrantLock();
        this.greenLightCondition = stateLock.newCondition();
        this.redLightCondition = stateLock.newCondition();
        this.pedestrianCrossingSemaphore = new Semaphore(MAX_CROSSING_PEDESTRIANS, true);
    }

    @Override
    public void run() {
        System.out.println("Semáforo " + id + " iniciado en posición: " + position);

        while (running) {
            try {
                long elapsed = System.currentTimeMillis() - lastChangeTime;
                long duration = getCurrentDuration();

                if (elapsed >= duration) {
                    changeToNextState();
                    lastChangeTime = System.currentTimeMillis(); // reset timer after state change
                }

                Thread.sleep(100); // check every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void changeToNextState() {
        stateLock.lock();
        try {
            switch (currentState) {
                case GREEN -> {
                    currentState = LightState.YELLOW;
                    // When changing from green to yellow, pedestrians should stop crossing
                    pedestrianCrossingSemaphore.drainPermits(); // Stop new pedestrians from crossing
                }
                case YELLOW -> {
                    currentState = LightState.RED;
                    // When changing to red, allow pedestrians to cross
                    pedestrianCrossingSemaphore.release(MAX_CROSSING_PEDESTRIANS); // Reset permits
                    redLightCondition.signalAll(); // Notify waiting pedestrians
                }
                case RED -> {
                    currentState = LightState.GREEN;
                    // When changing to green, stop pedestrians from crossing
                    pedestrianCrossingSemaphore.drainPermits(); // Stop pedestrians
                    greenLightCondition.signalAll(); // Notify waiting vehicles
                    System.out.println("Semáforo " + id + " señaló a todos los carros en espera");
                }
            }
            System.out.println("Semáforo " + id + " cambió a: " + currentState);
        } finally {
            stateLock.unlock();
        }
    }

    // Return the correct duration based on the current state
    private long getCurrentDuration() {
        return switch (currentState) {
            case GREEN -> greenLightTimer * 1000L;
            case YELLOW -> yellowLightTimer * 1000L;
            case RED -> redLightTimer * 1000L;
        };
    }

    public void stopSemaphore() {
        stopAgent();
        stateLock.lock();
        try {
            greenLightCondition.signalAll();
            redLightCondition.signalAll();
        } finally {
            stateLock.unlock();
        }
    }

    public LightState getCurrentState() {
        stateLock.lock();
        try {
            return currentState;
        } finally {
            stateLock.unlock();
        }
    }

    public Position getPosition() {
        return position;
    }

    // Called from TrafficSimulationCore
    public static void setLightsTimer(int[] timers) {
        greenLightTimer = timers[0];
        yellowLightTimer = timers[1];
        redLightTimer = timers[2];
    }

    // Method for cars to wait for green light
    public void waitForGreenLight() throws InterruptedException {
        stateLock.lock();
        try {
            while (currentState != LightState.GREEN && running) {
                System.out.println("Carro esperando en semáforo " + id + " (estado: " + currentState + ")");
                greenLightCondition.await(); // Wait until signaled
            }
            System.out.println("Carro puede avanzar en semáforo " + id + " (estado: " + currentState + ")");
        } finally {
            stateLock.unlock();
        }
    }

    // Method for pedestrians to wait for red light and cross
    public boolean waitForRedLightAndCross() throws InterruptedException {
        stateLock.lock();
        try {
            // Wait for red light
            while (currentState != LightState.RED && running) {
                System.out.println("Peatón esperando semáforo rojo en " + id + " (estado: " + currentState + ")");
                redLightCondition.await();
            }

            // Try to acquire crossing permit (only N pedestrians can cross simultaneously)
            if (pedestrianCrossingSemaphore.tryAcquire()) {
                System.out.println("Peatón " + Thread.currentThread().getName() + " comenzó a cruzar en semáforo " + id);
                return true;
            } else {
                System.out.println("Peatón " + Thread.currentThread().getName() + " no pudo cruzar - máximo alcanzado en semáforo " + id);
                return false;
            }
        } finally {
            stateLock.unlock();
        }
    }

    // Method for pedestrians to release crossing permit after crossing
    public void finishCrossing() {
        pedestrianCrossingSemaphore.release();
        System.out.println("Peatón " + Thread.currentThread().getName() + " terminó de cruzar en semáforo " + id);
    }

    // Get the crossing time (red light duration)
    public long getCrossingTime() {
        return redLightTimer * 1000L; // Return crossing time in milliseconds
    }
}