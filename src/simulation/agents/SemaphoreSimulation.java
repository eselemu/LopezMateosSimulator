package simulation.agents;

import simulation.map.Position;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreSimulation extends Agent {
    private static int redLightTimer;
    private static int yellowLightTimer;
    private static int greenLightTimer;
    
    public enum LightState {
        GREEN(greenLightTimer * 1000L),    // Time in seconds, converted into ms
        YELLOW(yellowLightTimer * 1000L),
        RED(redLightTimer * 1000L);

        private final long duration;

        LightState(long duration) {
            this.duration = duration;
        }

        public long getDuration() {
            return duration;
        }
    }
    
    

    private LightState currentState;
    private Position position;
    private long lastChangeTime;

    // New: Condition for waiting cars
    private final ReentrantLock stateLock;
    private final Condition greenLightCondition;

    public SemaphoreSimulation(int id, Position position){
        this.id = id;
        this.position = position;
        this.type = AgentType.SEMAPHORE;
        this.state = AgentState.ACTIVE;
        this.currentState = LightState.RED; // Empezar en rojo
        this.lastChangeTime = System.currentTimeMillis();

        // Initialize lock and condition
        this.stateLock = new ReentrantLock();
        this.greenLightCondition = stateLock.newCondition();
    }

    @Override
    public void run(){
        System.out.println("Semáforo " + id + " iniciado en posición: " + position);

        while(running) {
            try {
                long currentTime = System.currentTimeMillis();
                long timeInState = currentTime - lastChangeTime;

                if(timeInState >= currentState.getDuration()) {
                    changeToNextState();
                }

                Thread.sleep(100); // Revisar cada 100ms

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void changeToNextState() {
        stateLock.lock();
        try {
            switch(currentState) {
                case GREEN:
                    currentState = LightState.YELLOW;
                    break;
                case YELLOW:
                    currentState = LightState.RED;
                    break;
                case RED:
                    currentState = LightState.GREEN;
                    // Signal all waiting cars when light turns green
                    greenLightCondition.signalAll();
                    System.out.println("Semáforo " + id + " señaló a todos los carros en espera");
                    break;
            }

            lastChangeTime = System.currentTimeMillis();
            System.out.println("Semáforo " + id + " cambió a: " + currentState);
        } finally {
            stateLock.unlock();
        }
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

    // New method for cars to check if they can proceed without waiting
    public boolean canProceed() {
        stateLock.lock();
        try {
            return currentState == LightState.GREEN;
        } finally {
            stateLock.unlock();
        }
    }

    public void stopSemaphore() {
        stopAgent();
        // Wake up all waiting cars when stopping
        stateLock.lock();
        try {
            greenLightCondition.signalAll();
        } finally {
            stateLock.unlock();
        }
    }

    // Getters para UI
    public LightState getCurrentState() {
        stateLock.lock();
        try {
            return currentState;
        } finally {
            stateLock.unlock();
        }
    }
    public Position getPosition() { return position; }
    
    public static void setLightsTimer(int[] timers){
        greenLightTimer = timers[0];
        yellowLightTimer = timers[1];
        redLightTimer = timers[2];
    }
}