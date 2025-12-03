package simulation.distributed;

import simulation.agents.SemaphoreSimulation;
import simulation.map.Position;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * RMI Server implementation for distributed semaphore access.
 * 
 * This class wraps a local SemaphoreSimulation and exposes it via RMI,
 * allowing remote agents (cars, pedestrians) to interact with semaphores
 * across the network.
 * 
 * Key Features:
 * - Wraps local SemaphoreSimulation for remote access
 * - Manages pedestrian crossing buffer (max 3 pedestrians simultaneously)
 * - Tracks waiting vehicles
 * - Thread-safe operations using ConcurrentHashMap
 * 
 * The server is automatically exported when instantiated (via UnicastRemoteObject).
 * It must be registered in an RMI Registry to be accessible remotely.
 * 
 * @author Distributed Traffic Simulation System
 * @version 1.0
 * @see ISemaphoreServer
 * @see SemaphoreRegistry
 * @see SemaphoreSimulation
 */
public class SemaphoreServer extends UnicastRemoteObject implements ISemaphoreServer {
    /** The local semaphore being exposed remotely */
    private final SemaphoreSimulation localSemaphore;
    
    /** Semaphore controlling pedestrian crossing buffer (max N pedestrians) */
    private final Semaphore pedestrianCrossingSemaphore;
    
    /** Map tracking waiting vehicles (carId -> timestamp) */
    private final ConcurrentHashMap<Integer, Long> waitingCars;
    
    /** Maximum number of pedestrians that can cross simultaneously */
    private static final int MAX_CROSSING_PEDESTRIANS = 3;

    /**
     * Create a new SemaphoreServer wrapping a local semaphore.
     * 
     * @param localSemaphore The local SemaphoreSimulation to expose remotely
     * @throws RemoteException if the remote object cannot be exported
     */
    public SemaphoreServer(SemaphoreSimulation localSemaphore) throws RemoteException {
        super();
        this.localSemaphore = localSemaphore;
        this.pedestrianCrossingSemaphore = new Semaphore(MAX_CROSSING_PEDESTRIANS, true);
        this.waitingCars = new ConcurrentHashMap<>();
    }

    @Override
    public LightStateDTO getCurrentState() throws RemoteException {
        SemaphoreSimulation.LightState state = localSemaphore.getCurrentState();
        LightStateDTO.State dtoState = LightStateDTO.State.valueOf(state.name());
        return new LightStateDTO(dtoState, localSemaphore.id);
    }

    @Override
    public boolean requestGreenLight(int carId) throws RemoteException {
        // Register car as waiting
        waitingCars.put(carId, System.currentTimeMillis());

        if (localSemaphore.getCurrentState() == SemaphoreSimulation.LightState.GREEN) {
            waitingCars.remove(carId);
            System.out.println("🟢 Semaphore Server (" + localSemaphore.id + "): cleared Car " + carId);
            return true;
        }

        System.out.println("🔴 Semaphore Server (" + localSemaphore.id + "):  must wait at semaphore Car: " + carId);
        return false;
    }

    @Override
    public boolean requestCrossing(int pedestrianId) throws RemoteException {
        if (localSemaphore.getCurrentState() == SemaphoreSimulation.LightState.RED) {
            if (pedestrianCrossingSemaphore.tryAcquire()) {
                System.out.println("🚶‍♂️‍➡️ Server Semaphore (" + localSemaphore.id + "): Pedestrian " + pedestrianId + " crossing");
                return true;
            }
        }
        return false;
    }

    @Override
    public void finishCrossing(int pedestrianId) throws RemoteException{
        pedestrianCrossingSemaphore.release();
        System.out.println("🚶‍ Server Semaphore ( " + localSemaphore.id + "): Pedestrian " + pedestrianId + " finished crossing");
    }

    @Override
    public PositionDTO getPosition() throws RemoteException {
        Position pos = localSemaphore.getPosition();
        return new PositionDTO(pos.x, pos.y);
    }

    @Override
    public boolean isAlive() throws RemoteException{
        return localSemaphore.getAgentState() == SemaphoreSimulation.AgentState.ACTIVE;
    }

}
