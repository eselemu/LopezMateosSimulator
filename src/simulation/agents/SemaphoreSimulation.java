package simulation.agents;

import simulation.map.Position;

public class SemaphoreSimulation extends Agent {
    public enum LightState {
        GREEN(5000),    // 5 segundos
        YELLOW(2000),   // 2 segundos
        RED(5000);      // 5 segundos

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

    public SemaphoreSimulation(int id, Position position){
        this.id = id;
        this.position = position;
        this.type = AgentType.SEMAPHORE;
        this.state = AgentState.ACTIVE;
        this.currentState = LightState.RED; // Empezar en rojo
        this.lastChangeTime = System.currentTimeMillis();
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
        switch(currentState) {
            case GREEN:
                currentState = LightState.YELLOW;
                break;
            case YELLOW:
                currentState = LightState.RED;
                break;
            case RED:
                currentState = LightState.GREEN;
                break;
        }

        lastChangeTime = System.currentTimeMillis();
        System.out.println("Semáforo " + id + " cambió a: " + currentState);
    }

    public void stopSemaphore() {
        stopAgent();
    }

    // Getters para UI
    public LightState getCurrentState() { return currentState; }
    public Position getPosition() { return position; }
}
