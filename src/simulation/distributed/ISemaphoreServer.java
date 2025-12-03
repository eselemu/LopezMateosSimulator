package simulation.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI Remote Interface for distributed semaphore servers.
 * 
 * This interface defines the contract for remote semaphore operations that can be
 * accessed across the network via Java RMI. It provides methods for:
 * - Querying semaphore state (GREEN, YELLOW, RED)
 * - Requesting green light permission (for vehicles)
 * - Requesting crossing permission (for pedestrians)
 * - Managing pedestrian crossing lifecycle
 * - Health checking
 * 
 * All methods must throw RemoteException as required by RMI.
 * 
 * @author Distributed Traffic Simulation System
 * @version 1.0
 * @see SemaphoreServer
 * @see SemaphoreRegistry
 */
public interface ISemaphoreServer extends Remote {
    
    /**
     * Get the current state of the semaphore light.
     * 
     * @return LightStateDTO containing the current state, semaphore ID, and timestamp
     * @throws RemoteException if the remote call fails
     */
    LightStateDTO getCurrentState() throws RemoteException;

    /**
     * Request green light permission for a vehicle.
     * This method is called by cars/trucks when they approach a semaphore.
     * 
     * @param carId The ID of the vehicle requesting permission
     * @return true if green light is granted (semaphore is GREEN), false if must wait
     * @throws RemoteException if the remote call fails
     */
    boolean requestGreenLight(int carId) throws RemoteException;

    /**
     * Request crossing permission for a pedestrian.
     * This method is called by pedestrians when they want to cross at a red light.
     * The semaphore controls how many pedestrians can cross simultaneously (buffer).
     * 
     * @param pedestrianId The ID of the pedestrian requesting permission
     * @return true if crossing is granted, false if denied (buffer full or not red light)
     * @throws RemoteException if the remote call fails
     */
    boolean requestCrossing(int pedestrianId) throws RemoteException;

    /**
     * Notify the semaphore that a pedestrian has finished crossing.
     * This releases the crossing permit, allowing another pedestrian to cross.
     * 
     * @param pedestrianId The ID of the pedestrian who finished crossing
     * @throws RemoteException if the remote call fails
     */
    void finishCrossing(int pedestrianId) throws RemoteException;

    /**
     * Get the position of the semaphore in the traffic map.
     * 
     * @return PositionDTO containing the x and y coordinates
     * @throws RemoteException if the remote call fails
     */
    PositionDTO getPosition() throws RemoteException;
    
    /**
     * Health check to verify the semaphore server is alive and active.
     * 
     * @return true if the semaphore is active, false otherwise
     * @throws RemoteException if the remote call fails
     */
    boolean isAlive() throws RemoteException;
}
