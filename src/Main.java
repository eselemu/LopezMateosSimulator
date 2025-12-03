import simulation.TrafficSimulationCore;
import simulation.ui.AgentVisualizer;
import simulation.ui.ThreadVisualizer;
import simulation.ui.TrafficSimulationUI;

/**
 * Unified Main - Supports both Local and Distributed modes.
 * 
 * Usage:
 * - Local Mode: Run without arguments (or with --local)
 * - Distributed Mode: Run with --distributed [port]
 * 
 * Examples:
 *   java Main                    # Local mode
 *   java Main --local            # Local mode (explicit)
 *   java Main --distributed      # Distributed mode (port 1099)
 *   java Main --distributed 1098 # Distributed mode (custom port)
 */
public class Main {
    // Default configuration
    private static final int DEFAULT_CARS = 10;
    private static final int DEFAULT_TRUCKS = 5;
    private static final int DEFAULT_PEDESTRIANS = 8;
    private static final int DEFAULT_GREEN_TIME = 5;
    private static final int DEFAULT_YELLOW_TIME = 2;
    private static final int DEFAULT_RED_TIME = 3;
    private static final int DEFAULT_RMI_PORT = 1099;
    
    public static void main(String[] args) {
        boolean distributedMode = false;
        boolean serverMode = false;
        boolean clientMode = false;
        int rmiPort = DEFAULT_RMI_PORT;
        String serverHost = "localhost";
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--distributed") || args[i].equals("-d")) {
                distributedMode = true;
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    try {
                        rmiPort = Integer.parseInt(args[i + 1]);
                        i++; // Skip next argument
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number, using default: " + DEFAULT_RMI_PORT);
                    }
                }
            } else if (args[i].equals("--server") || args[i].equals("-s")) {
                serverMode = true;
                distributedMode = true;
            } else if (args[i].equals("--client") || args[i].equals("-c")) {
                clientMode = true;
                distributedMode = true;
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    serverHost = args[i + 1];
                    i++; // Skip next argument
                }
            } else if (args[i].equals("--local") || args[i].equals("-l")) {
                distributedMode = false;
            } else if (args[i].equals("--help") || args[i].equals("-h")) {
                printUsage();
                return;
            }
        }
        
        System.out.println("========================================");
        System.out.println("Lopez Mateos Traffic Simulator");
        if (serverMode) {
            System.out.println("SERVER MODE - Semaphores Only");
        } else if (clientMode) {
            System.out.println("CLIENT MODE - Agents Only");
            System.out.println("Connecting to: " + serverHost + ":" + rmiPort);
        } else {
            System.out.println(distributedMode ? "Distributed Mode" : "Local Mode");
        }
        System.out.println("========================================");
        System.out.println();
        
        TrafficSimulationCore simulation = TrafficSimulationCore.getInstance();
        
        // Server mode: Only semaphores, no agents
        if (serverMode) {
            System.out.println("Initializing SERVER (semaphores only)...");
            //simulation.initializeSimulation(0, 0, 0, 0, DEFAULT_GREEN_TIME, DEFAULT_YELLOW_TIME, DEFAULT_RED_TIME);
            System.out.println("Starting RMI Registry on port " + rmiPort + "...");
            boolean connected = simulation.connectToTrafficManager(rmiPort);
            if (!connected) {
                System.err.println("❌ Failed to start server mode");
                return;
            }
            System.out.println("✅ SERVER running - Waiting for clients to connect...");
            System.out.println("   Semaphores registered and ready");
        }
        // Client mode: Only agents, connect to remote server
        else if (clientMode) {
            System.out.println("Initializing CLIENT (agents only)...");
//            simulation.initializeSimulation(
//                DEFAULT_CARS,
//                DEFAULT_TRUCKS,
//                0, // No local semaphores
//                DEFAULT_PEDESTRIANS,
//                DEFAULT_GREEN_TIME,
//                DEFAULT_YELLOW_TIME,
//                DEFAULT_RED_TIME
//            );
            // Configure client to connect to remote server
            simulation.getDistributedClient().configureDefaults(serverHost, rmiPort);
            System.out.println("✅ CLIENT configured - Connecting to server at " + serverHost + ":" + rmiPort);
        }
        // Normal mode: Full simulation
        else {
            System.out.println("Initializing simulation...");
//            simulation.initializeSimulation(
//                DEFAULT_CARS,
//                DEFAULT_TRUCKS,
//                0, // Semaphores are created from map
//                DEFAULT_PEDESTRIANS,
//                DEFAULT_GREEN_TIME,
//                DEFAULT_YELLOW_TIME,
//                DEFAULT_RED_TIME
//            );
            
            // Enable distributed mode if requested
            if (distributedMode) {
                System.out.println("Enabling distributed mode on port " + rmiPort + "...");
                boolean connected = simulation.connectToTrafficManager(rmiPort);
                if (!connected) {
                    System.err.println("⚠️ Failed to start distributed mode, continuing in local mode");
                } else {
                    System.out.println("✅ Distributed mode enabled");
                }
            } else {
                System.out.println("Running in local mode (no RMI)");
            }
        }
        
        System.out.println();
        if (!serverMode) {
            System.out.println("Starting simulation...");
            System.out.println("Press Ctrl+C to stop");
            System.out.println("========================================");
            System.out.println();
            // Start simulation (only if not server mode)
            simulation.startSimulation();
        } else {
            System.out.println("SERVER is running and waiting for clients...");
            System.out.println("Press Ctrl+C to stop the server");
            System.out.println("========================================");
            System.out.println();
        }
        
        // UI (only if not server mode)
        if (!serverMode) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                TrafficSimulationUI ui = new TrafficSimulationUI(simulation);
                ui.setVisible(true);
            });
            
            // Visualizers
            ThreadVisualizer threadVisualizer = new ThreadVisualizer();
            Thread threadVisualizerThread = new Thread(threadVisualizer);
            threadVisualizerThread.start();
            
            AgentVisualizer agentVisualizer = new AgentVisualizer();
            Thread agentVisualizerThread = new Thread(agentVisualizer);
            agentVisualizerThread.start();
        }
        
        // Keep running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("\nSimulation interrupted, shutting down...");
        } finally {
            if (serverMode) {
                System.out.println("Stopping server...");
                simulation.disconnectFromTrafficManager();
                System.out.println("Server stopped.");
            } else {
                System.out.println("Stopping simulation...");
                simulation.stopSimulation();
                if (distributedMode) {
                    simulation.disconnectFromTrafficManager();
                }
                System.out.println("Simulation stopped.");
            }
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java Main [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --local, -l              Run in local mode (default)");
        System.out.println("  --distributed, -d [port] Run in distributed mode (default port: 1099)");
        System.out.println("  --server, -s             Run as server (semaphores only)");
        System.out.println("  --client, -c [host]      Run as client (agents only, connects to server)");
        System.out.println("  --help, -h               Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java Main                          # Local mode");
        System.out.println("  java Main --distributed            # Distributed mode (port 1099)");
        System.out.println("  java Main --server                 # Server mode (semaphores only)");
        System.out.println("  java Main --client localhost       # Client mode (connect to localhost)");
        System.out.println("  java Main --client 192.168.1.100    # Client mode (connect to remote server)");
    }
}
