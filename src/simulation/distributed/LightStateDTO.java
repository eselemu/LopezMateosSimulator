package simulation.distributed;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) for semaphore light state.
 * 
 * This class is used to transfer semaphore state information across the network
 * via RMI. It implements Serializable to allow network transmission.
 * 
 * Contains:
 * - Current light state (GREEN, YELLOW, RED)
 * - Semaphore ID for identification
 * - Timestamp of when the state was captured
 * 
 * @author Distributed Traffic Simulation System
 * @version 1.0
 */
public class LightStateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enumeration of possible semaphore light states
     */
    public enum State {
        /** Green light - vehicles can proceed */
        GREEN,
        /** Yellow light - warning, prepare to stop */
        YELLOW,
        /** Red light - vehicles must stop, pedestrians can cross */
        RED
    }

    /** Current state of the semaphore light */
    public State currentState;
    
    /** Timestamp when this state was captured (milliseconds since epoch) */
    public long timestamp;
    
    /** Unique identifier of the semaphore */
    public int semaphoreId;

    /**
     * Create a new LightStateDTO with the current timestamp.
     * 
     * @param currentState The current light state
     * @param semaphoreId The semaphore identifier
     */
    public LightStateDTO(State currentState, int semaphoreId) {
        this.currentState = currentState;
        this.semaphoreId = semaphoreId;
        this.timestamp = System.currentTimeMillis();
    }
}
