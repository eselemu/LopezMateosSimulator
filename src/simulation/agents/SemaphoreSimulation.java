package simulation.agents;

import simulation.map.Position;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreSimulation extends Agent {
    private static int redLightTimer;
    private static int yellowLightTimer;
    private static int greenLightTimer;

    public enum LightState { GREEN, YELLOW, RED }

    private LightState currentState;
    private Position position;
    private long lastChangeTime;

    private final ReentrantLock stateLock;
    private final Condition greenLightCondition;

    public SemaphoreSimulation(int id, Position position) {
        this.id = id;
        this.position = position;
        this.type = AgentType.SEMAPHORE;
        this.state = AgentState.ACTIVE;
        this.currentState = LightState.RED; // Start at red
        this.lastChangeTime = System.currentTimeMillis();

        this.stateLock = new ReentrantLock();
        this.greenLightCondition = stateLock.newCondition();
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
                case GREEN -> currentState = LightState.YELLOW;
                case YELLOW -> currentState = LightState.RED;
                case RED -> {
                    currentState = LightState.GREEN;
                    greenLightCondition.signalAll();
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

    // New method for cars to wait for green light
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
}
