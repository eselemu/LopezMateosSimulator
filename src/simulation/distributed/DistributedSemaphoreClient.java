package simulation.distributed;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client wrapper for connecting to remote semaphore servers via RMI.
 * This class provides a unified interface for agents (Cars, Pedestrians) to interact
 * with semaphores, whether they are local or remote.
 * 
 * Features:
 * - Automatic connection management and caching
 * - Retry logic for failed connections
 * - Fallback to local semaphores if remote connection fails
 * - Thread-safe operations
 * 
 * @author Distributed Traffic Simulation System
 * @version 1.0
 */
public class DistributedSemaphoreClient {
    private static DistributedSemaphoreClient instance;
    
    // Cache of remote semaphore connections
    private final ConcurrentHashMap<Integer, ISemaphoreServer> remoteSemaphoreCache;
    private final ConcurrentHashMap<Integer, String> semaphoreHosts;
    private final ConcurrentHashMap<Integer, Integer> semaphorePorts;
    
    // Default connection parameters
    private String defaultHost;
    private int defaultPort;
    private int maxRetries;
    private long retryDelayMs;
    
    // Statistics
    private final AtomicInteger successfulConnections;
    private final AtomicInteger failedConnections;
    private final AtomicInteger cacheHits;
    private final AtomicInteger cacheMisses;

    /**
     * Default connection parameters
     */
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1099;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;

    private DistributedSemaphoreClient() {
        this.remoteSemaphoreCache = new ConcurrentHashMap<>();
        this.semaphoreHosts = new ConcurrentHashMap<>();
        this.semaphorePorts = new ConcurrentHashMap<>();
        this.defaultHost = DEFAULT_HOST;
        this.defaultPort = DEFAULT_PORT;
        this.maxRetries = DEFAULT_MAX_RETRIES;
        this.retryDelayMs = DEFAULT_RETRY_DELAY_MS;
        
        this.successfulConnections = new AtomicInteger(0);
        this.failedConnections = new AtomicInteger(0);
        this.cacheHits = new AtomicInteger(0);
        this.cacheMisses = new AtomicInteger(0);
    }

    /**
     * Get singleton instance of DistributedSemaphoreClient
     * @return The singleton instance
     */
    public static synchronized DistributedSemaphoreClient getInstance() {
        if (instance == null) {
            instance = new DistributedSemaphoreClient();
        }
        return instance;
    }

    /**
     * Configure default connection parameters
     * @param host Default hostname
     * @param port Default port
     */
    public void configureDefaults(String host, int port) {
        this.defaultHost = host != null ? host : DEFAULT_HOST;
        this.defaultPort = port > 0 ? port : DEFAULT_PORT;
        System.out.println("🔧 DistributedSemaphoreClient configured: " + defaultHost + ":" + defaultPort);
    }

    /**
     * Register a semaphore's location for future lookups
     * @param semaphoreId The semaphore ID
     * @param host The host where the semaphore is registered
     * @param port The port of the RMI registry
     */
    public void registerSemaphoreLocation(int semaphoreId, String host, int port) {
        semaphoreHosts.put(semaphoreId, host != null ? host : defaultHost);
        semaphorePorts.put(semaphoreId, port > 0 ? port : defaultPort);
    }

    /**
     * Get or create a connection to a remote semaphore
     * @param semaphoreId The semaphore ID
     * @return The remote semaphore server interface, or null if connection fails
     */
    public ISemaphoreServer getRemoteSemaphore(int semaphoreId) {
        // Check cache first
        ISemaphoreServer cached = remoteSemaphoreCache.get(semaphoreId);
        if (cached != null) {
            // Verify connection is still alive
            try {
                if (cached.isAlive()) {
                    cacheHits.incrementAndGet();
                    return cached;
                } else {
                    // Connection is dead, remove from cache
                    remoteSemaphoreCache.remove(semaphoreId);
                }
            } catch (RemoteException e) {
                // Connection failed, remove from cache
                remoteSemaphoreCache.remove(semaphoreId);
            }
        }

        cacheMisses.incrementAndGet();
        
        // Get host and port for this semaphore
        String host = semaphoreHosts.getOrDefault(semaphoreId, defaultHost);
        int port = semaphorePorts.getOrDefault(semaphoreId, defaultPort);
        
        // Try to connect with retries
        ISemaphoreServer remoteServer = connectWithRetry(semaphoreId, host, port);
        
        if (remoteServer != null) {
            remoteSemaphoreCache.put(semaphoreId, remoteServer);
            successfulConnections.incrementAndGet();
            return remoteServer;
        } else {
            failedConnections.incrementAndGet();
            return null;
        }
    }

    /**
     * Connect to a remote semaphore with retry logic
     * @param semaphoreId The semaphore ID
     * @param host The host
     * @param port The port
     * @return The remote server interface, or null if all retries fail
     */
    private ISemaphoreServer connectWithRetry(int semaphoreId, String host, int port) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ISemaphoreServer server = SemaphoreRegistry.lookupRemoteSemaphore(semaphoreId, host, port);
                System.out.println("✅ Connected to remote Semaphore " + semaphoreId + 
                                 " (attempt " + attempt + "/" + maxRetries + ")");
                return server;
            } catch (RemoteException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    System.out.println("⚠️ Failed to connect to Semaphore " + semaphoreId + 
                                     " (attempt " + attempt + "/" + maxRetries + 
                                     "), retrying in " + retryDelayMs + "ms...");
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        
        System.err.println("❌ Failed to connect to remote Semaphore " + semaphoreId + 
                         " after " + maxRetries + " attempts: " + 
                         (lastException != null ? lastException.getMessage() : "Unknown error"));
        return null;
    }

    /**
     * Request green light from a remote semaphore (for cars)
     * @param semaphoreId The semaphore ID
     * @param carId The car ID making the request
     * @return true if green light is granted, false otherwise
     */
    public boolean requestGreenLight(int semaphoreId, int carId) {
        ISemaphoreServer remoteServer = getRemoteSemaphore(semaphoreId);
        if (remoteServer == null) {
            System.err.println("⚠️ Cannot request green light: Semaphore " + semaphoreId + " is not available");
            return false;
        }

        try {
            return remoteServer.requestGreenLight(carId);
        } catch (RemoteException e) {
            System.err.println("❌ Error requesting green light from Semaphore " + semaphoreId + 
                             ": " + e.getMessage());
            // Remove from cache on error
            remoteSemaphoreCache.remove(semaphoreId);
            return false;
        }
    }

    /**
     * Request crossing permission from a remote semaphore (for pedestrians)
     * @param semaphoreId The semaphore ID
     * @param pedestrianId The pedestrian ID making the request
     * @return true if crossing is granted, false otherwise
     */
    public boolean requestCrossing(int semaphoreId, int pedestrianId) {
        ISemaphoreServer remoteServer = getRemoteSemaphore(semaphoreId);
        if (remoteServer == null) {
            System.err.println("⚠️ Cannot request crossing: Semaphore " + semaphoreId + " is not available");
            return false;
        }

        try {
            return remoteServer.requestCrossing(pedestrianId);
        } catch (RemoteException e) {
            System.err.println("❌ Error requesting crossing from Semaphore " + semaphoreId + 
                             ": " + e.getMessage());
            // Remove from cache on error
            remoteSemaphoreCache.remove(semaphoreId);
            return false;
        }
    }

    /**
     * Notify a remote semaphore that a pedestrian has finished crossing
     * @param semaphoreId The semaphore ID
     * @param pedestrianId The pedestrian ID
     */
    public void finishCrossing(int semaphoreId, int pedestrianId) {
        ISemaphoreServer remoteServer = getRemoteSemaphore(semaphoreId);
        if (remoteServer == null) {
            System.err.println("⚠️ Cannot finish crossing: Semaphore " + semaphoreId + " is not available");
            return;
        }

        try {
            remoteServer.finishCrossing(pedestrianId);
        } catch (RemoteException e) {
            System.err.println("❌ Error finishing crossing at Semaphore " + semaphoreId + 
                             ": " + e.getMessage());
            // Remove from cache on error
            remoteSemaphoreCache.remove(semaphoreId);
        }
    }

    /**
     * Get the current state of a remote semaphore
     * @param semaphoreId The semaphore ID
     * @return The light state DTO, or null if unavailable
     */
    public LightStateDTO getCurrentState(int semaphoreId) {
        ISemaphoreServer remoteServer = getRemoteSemaphore(semaphoreId);
        if (remoteServer == null) {
            return null;
        }

        try {
            return remoteServer.getCurrentState();
        } catch (RemoteException e) {
            System.err.println("❌ Error getting state from Semaphore " + semaphoreId + 
                             ": " + e.getMessage());
            // Remove from cache on error
            remoteSemaphoreCache.remove(semaphoreId);
            return null;
        }
    }

    /**
     * Get the position of a remote semaphore
     * @param semaphoreId The semaphore ID
     * @return The position DTO, or null if unavailable
     */
    public PositionDTO getPosition(int semaphoreId) {
        ISemaphoreServer remoteServer = getRemoteSemaphore(semaphoreId);
        if (remoteServer == null) {
            return null;
        }

        try {
            return remoteServer.getPosition();
        } catch (RemoteException e) {
            System.err.println("❌ Error getting position from Semaphore " + semaphoreId + 
                             ": " + e.getMessage());
            // Remove from cache on error
            remoteSemaphoreCache.remove(semaphoreId);
            return null;
        }
    }

    /**
     * Check if a remote semaphore is alive
     * @param semaphoreId The semaphore ID
     * @return true if alive, false otherwise
     */
    public boolean isAlive(int semaphoreId) {
        ISemaphoreServer remoteServer = getRemoteSemaphore(semaphoreId);
        if (remoteServer == null) {
            return false;
        }

        try {
            return remoteServer.isAlive();
        } catch (RemoteException e) {
            // Remove from cache on error
            remoteSemaphoreCache.remove(semaphoreId);
            return false;
        }
    }

    /**
     * Clear the connection cache (useful for reconnection scenarios)
     */
    public void clearCache() {
        remoteSemaphoreCache.clear();
        System.out.println("🧹 DistributedSemaphoreClient cache cleared");
    }

    /**
     * Remove a specific semaphore from the cache
     * @param semaphoreId The semaphore ID
     */
    public void removeFromCache(int semaphoreId) {
        remoteSemaphoreCache.remove(semaphoreId);
    }

    /**
     * Get connection statistics
     * @return A string with statistics
     */
    public String getStatistics() {
        return String.format(
            "DistributedSemaphoreClient Stats:\n" +
            "  Successful Connections: %d\n" +
            "  Failed Connections: %d\n" +
            "  Cache Hits: %d\n" +
            "  Cache Misses: %d\n" +
            "  Cached Semaphores: %d",
            successfulConnections.get(),
            failedConnections.get(),
            cacheHits.get(),
            cacheMisses.get(),
            remoteSemaphoreCache.size()
        );
    }

    /**
     * Print connection statistics to console
     */
    public void printStatistics() {
        System.out.println(getStatistics());
    }
}

