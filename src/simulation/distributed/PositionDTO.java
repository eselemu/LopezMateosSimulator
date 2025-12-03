package simulation.distributed;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) for position coordinates.
 * 
 * This class is used to transfer position information across the network
 * via RMI. It implements Serializable to allow network transmission.
 * 
 * Represents a 2D coordinate in the traffic simulation map.
 * 
 * @author Distributed Traffic Simulation System
 * @version 1.0
 */
public class PositionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** X coordinate in the traffic map */
    public int x;
    
    /** Y coordinate in the traffic map */
    public int y;

    /**
     * Create a new PositionDTO with the specified coordinates.
     * 
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public PositionDTO(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
