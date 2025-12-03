package simulation.distributed;

import simulation.agents.SemaphoreSimulation;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the RMI Registry for distributed semaphore servers.
 * This class handles:
 * - Starting/stopping the RMI registry
 * - Registering semaphore servers with unique names
 * - Providing lookup functionality for remote semaphore clients
 * - Managing the lifecycle of distributed semaphore services
 * 
 * @author Distributed Traffic Simulation System
 * @version 1.0
 */
public class SemaphoreRegistry {
    private static SemaphoreRegistry instance;
    private Registry rmiRegistry;
    private Map<Integer, SemaphoreServer> registeredServers;
    private Map<Integer, String> semaphoreBindings; // Maps semaphore ID to RMI binding name
    private int registryPort;
    private boolean isRunning;

    /**
     * Default RMI registry port
     */
    private static final int DEFAULT_REGISTRY_PORT = 1099;

    /**
     * Base name for semaphore RMI bindings
     */
    private static final String SEMAPHORE_BINDING_PREFIX = "SemaphoreServer_";

    private SemaphoreRegistry() {
        this.registeredServers = new ConcurrentHashMap<>();
        this.semaphoreBindings = new ConcurrentHashMap<>();
        this.registryPort = DEFAULT_REGISTRY_PORT;
        this.isRunning = false;
    }

    /**
     * Get singleton instance of SemaphoreRegistry
     * @return The singleton instance
     */
    public static synchronized SemaphoreRegistry getInstance() {
        if (instance == null) {
            instance = new SemaphoreRegistry();
        }
        return instance;
    }

    /**
     * Start the RMI registry on the default port (1099)
     * @throws RemoteException if the registry cannot be started
     */
    public void startRegistry() throws RemoteException {
        startRegistry(DEFAULT_REGISTRY_PORT);
    }

    /**
     * Start the RMI registry on a specific port
     * @param port The port number for the RMI registry
     * @throws RemoteException if the registry cannot be started
     */
    public void startRegistry(int port) throws RemoteException {
        if (isRunning) {
            System.out.println("⚠️ RMI Registry is already running on port " + registryPort);
            return;
        }

        try {
            // Try to get existing registry first
            rmiRegistry = LocateRegistry.getRegistry(port);
            rmiRegistry.list(); // Test if registry is accessible
            System.out.println("✅ Connected to existing RMI Registry on port " + port);
        } catch (RemoteException e) {
            // If registry doesn't exist, create a new one
            try {
                rmiRegistry = LocateRegistry.createRegistry(port);
                System.out.println("✅ Created new RMI Registry on port " + port);
            } catch (RemoteException ex) {
                System.err.println("❌ Failed to create RMI Registry on port " + port + ": " + ex.getMessage());
                throw ex;
            }
        }

        this.registryPort = port;
        this.isRunning = true;
        System.out.println("🚀 RMI Registry started successfully on port " + port);
    }

    /**
     * Register a semaphore server in the RMI registry
     * @param semaphore The local semaphore to register
     * @return The registered SemaphoreServer instance
     * @throws RemoteException if registration fails
     */
    public SemaphoreServer registerSemaphore(SemaphoreSimulation semaphore) throws RemoteException {
        if (!isRunning) {
            throw new IllegalStateException("RMI Registry is not running. Call startRegistry() first.");
        }

        if (registeredServers.containsKey(semaphore.id)) {
            System.out.println("⚠️ Semaphore " + semaphore.id + " is already registered");
            return registeredServers.get(semaphore.id);
        }

        try {
            // Create RMI server wrapper
            SemaphoreServer server = new SemaphoreServer(semaphore);
            
            // Create unique binding name
            String bindingName = SEMAPHORE_BINDING_PREFIX + semaphore.id;
            
            // Export and bind to registry
            rmiRegistry.rebind(bindingName, server);
            
            // Store references
            registeredServers.put(semaphore.id, server);
            semaphoreBindings.put(semaphore.id, bindingName);
            
            System.out.println("✅ Registered Semaphore " + semaphore.id + 
                             " as '" + bindingName + "' in RMI Registry");
            
            return server;
        } catch (RemoteException e) {
            System.err.println("❌ Failed to register Semaphore " + semaphore.id + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Register multiple semaphores at once
     * @param semaphores List of semaphores to register
     * @return Map of semaphore IDs to their registered servers
     * @throws RemoteException if registration fails
     */
    public Map<Integer, SemaphoreServer> registerSemaphores(List<SemaphoreSimulation> semaphores) 
            throws RemoteException {
        Map<Integer, SemaphoreServer> registered = new HashMap<>();
        
        for (SemaphoreSimulation semaphore : semaphores) {
            try {
                SemaphoreServer server = registerSemaphore(semaphore);
                registered.put(semaphore.id, server);
            } catch (RemoteException e) {
                System.err.println("⚠️ Failed to register semaphore " + semaphore.id + 
                                 ", continuing with others...");
            }
        }
        
        System.out.println("✅ Registered " + registered.size() + " out of " + 
                         semaphores.size() + " semaphores");
        
        return registered;
    }

    /**
     * Unregister a semaphore from the RMI registry
     * @param semaphoreId The ID of the semaphore to unregister
     * @throws RemoteException if unregistration fails
     */
    public void unregisterSemaphore(int semaphoreId) throws RemoteException {
        if (!registeredServers.containsKey(semaphoreId)) {
            System.out.println("⚠️ Semaphore " + semaphoreId + " is not registered");
            return;
        }

        try {
            String bindingName = semaphoreBindings.get(semaphoreId);
            rmiRegistry.unbind(bindingName);
            
            // Unexport the remote object
            SemaphoreServer server = registeredServers.get(semaphoreId);
            UnicastRemoteObject.unexportObject(server, true);
            
            registeredServers.remove(semaphoreId);
            semaphoreBindings.remove(semaphoreId);
            
            System.out.println("✅ Unregistered Semaphore " + semaphoreId + 
                             " from RMI Registry");
        } catch (Exception e) {
            System.err.println("❌ Failed to unregister Semaphore " + semaphoreId + ": " + e.getMessage());
            throw new RemoteException("Unregistration failed", e);
        }
    }

    /**
     * Get a reference to a registered semaphore server
     * @param semaphoreId The ID of the semaphore
     * @return The SemaphoreServer instance, or null if not registered
     */
    public SemaphoreServer getRegisteredServer(int semaphoreId) {
        return registeredServers.get(semaphoreId);
    }

    /**
     * Get the RMI binding name for a semaphore
     * @param semaphoreId The ID of the semaphore
     * @return The binding name, or null if not registered
     */
    public String getBindingName(int semaphoreId) {
        return semaphoreBindings.get(semaphoreId);
    }

    /**
     * Lookup a remote semaphore server from the registry
     * @param semaphoreId The ID of the semaphore to lookup
     * @param host The host where the registry is running (null for localhost)
     * @param port The port of the registry
     * @return The remote ISemaphoreServer interface
     * @throws RemoteException if lookup fails
     */
    public static ISemaphoreServer lookupRemoteSemaphore(int semaphoreId, String host, int port) 
            throws RemoteException {
        try {
            Registry registry;
            if (host == null || host.isEmpty()) {
                registry = LocateRegistry.getRegistry(port);
            } else {
                registry = LocateRegistry.getRegistry(host, port);
            }
            
            String bindingName = SEMAPHORE_BINDING_PREFIX + semaphoreId;
            ISemaphoreServer remoteServer = (ISemaphoreServer) registry.lookup(bindingName);
            
            System.out.println("✅ Successfully looked up remote Semaphore " + semaphoreId + 
                             " from " + (host != null ? host : "localhost") + ":" + port);
            
            return remoteServer;
        } catch (Exception e) {
            System.err.println("❌ Failed to lookup remote Semaphore " + semaphoreId + 
                             " from " + (host != null ? host : "localhost") + ":" + port + 
                             ": " + e.getMessage());
            throw new RemoteException("Lookup failed", e);
        }
    }

    /**
     * Stop the RMI registry and unregister all semaphores
     */
    public void stopRegistry() {
        if (!isRunning) {
            System.out.println("⚠️ RMI Registry is not running");
            return;
        }

        try {
            // Unregister all semaphores
            for (Integer semaphoreId : new HashMap<>(registeredServers).keySet()) {
                try {
                    unregisterSemaphore(semaphoreId);
                } catch (RemoteException e) {
                    System.err.println("⚠️ Error unregistering semaphore " + semaphoreId);
                }
            }

            // Note: We cannot actually stop the registry as it's managed by the JVM
            // But we can clear our references
            registeredServers.clear();
            semaphoreBindings.clear();
            rmiRegistry = null;
            isRunning = false;
            
            System.out.println("✅ RMI Registry stopped (all semaphores unregistered)");
        } catch (Exception e) {
            System.err.println("❌ Error stopping RMI Registry: " + e.getMessage());
        }
    }

    /**
     * Check if the registry is running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get the current registry port
     * @return The port number
     */
    public int getRegistryPort() {
        return registryPort;
    }

    /**
     * Get the number of registered semaphores
     * @return The count
     */
    public int getRegisteredCount() {
        return registeredServers.size();
    }

    /**
     * List all registered semaphore IDs
     * @return Array of semaphore IDs
     */
    public int[] getRegisteredSemaphoreIds() {
        return registeredServers.keySet().stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }
}

