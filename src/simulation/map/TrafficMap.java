package simulation.map;

import simulation.agents.SemaphoreSimulation;
import java.util.*;

public class TrafficMap {
    private Map<String, TrafficNode> nodes;
    private Map<String, TrafficEdge> edges;

    // Representación en grid para visualización
    private int width, height;
    private TrafficNode[][] grid;

    private int scale = 1;

    public TrafficMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
        this.grid = new TrafficNode[width][height];
        initializeGridMap();
    }

    private void initializeSimpleMap(){

    }

    private void initializeGridMap() {
        boolean toggle = true;
        // Create nodes in a grid pattern
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TrafficNode.NodeType type = determineNodeType(x, y);
                String nodeId = "N_" + x + "_" + y;
                Position pos = new Position(x * scale, y * scale); // Scale for visualization

                TrafficNode node = new TrafficNode(nodeId, pos, type);
                nodes.put(nodeId, node);
                grid[x][y] = node;

                // Add semaphore at intersections
                if (type == TrafficNode.NodeType.INTERSECTION) {
                    toggle = !toggle;
                    if(toggle){
                        SemaphoreSimulation semaphore = new SemaphoreSimulation(
                                Integer.parseInt(x + "" + y), pos);
                        node.setSemaphore(semaphore);
                    }
                }
            }
        }

        createGridEdges();
    }

    private TrafficNode.NodeType determineNodeType(int x, int y) {
        // Intersections at even coordinates, streets in between
        if (x % 2 == 0 && y % 2 == 0) {
            return TrafficNode.NodeType.INTERSECTION;
        } else if (x % 2 == 1 && y % 2 == 0) {
            return TrafficNode.NodeType.STREET; // Horizontal street
        } else if (x % 2 == 0 && y % 2 == 1) {
            return TrafficNode.NodeType.STREET; // Vertical street
        } else {
            return TrafficNode.NodeType.CROSSWALK; // Crosswalk positions
        }
    }

    private void createGridEdges() {
        // Create horizontal edges
        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width - 2; x += 2) {
                TrafficNode from = grid[x][y];
                TrafficNode to = grid[x + 2][y];

                if (from != null && to != null) {
                    // Create edge with intermediate street node
                    TrafficNode streetNode = grid[x + 1][y];
                    createEdgeWithIntermediate(from, streetNode, to, "H_" + x + "_" + y);
                }
            }
        }

        // Create vertical edges
        for (int x = 0; x < width; x += 2) {
            for (int y = 0; y < height - 2; y += 2) {
                TrafficNode from = grid[x][y];
                TrafficNode to = grid[x][y + 2];

                if (from != null && to != null) {
                    // Create edge with intermediate street node
                    TrafficNode streetNode = grid[x][y + 1];
                    createEdgeWithIntermediate(from, streetNode, to, "V_" + x + "_" + y);
                }
            }
        }
    }

    private void createEdgeWithIntermediate(TrafficNode from, TrafficNode intermediate, TrafficNode to, String baseId) {
        // Edge from intersection to street
        TrafficEdge edge1 = new TrafficEdge(baseId + "_1", from, intermediate, scale);
        edges.put(edge1.getEdgeId(), edge1);
        from.addOutgoingEdge(edge1);
        intermediate.addIncomingEdge(edge1);

        // Edge from street to intersection
        TrafficEdge edge2 = new TrafficEdge(baseId + "_2", intermediate, to, scale);
        edges.put(edge2.getEdgeId(), edge2);
        intermediate.addOutgoingEdge(edge2);
        to.addIncomingEdge(edge2);
    }

    public List<TrafficNode> findShortestPath(TrafficNode start, TrafficNode end) {
        // Dijkstra's algorithm implementation
        Map<TrafficNode, Double> distances = new HashMap<>();
        Map<TrafficNode, TrafficNode> previous = new HashMap<>();
        PriorityQueue<TrafficNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(node -> distances.getOrDefault(node, Double.MAX_VALUE))
        );

        // Initialize
        for (TrafficNode node : nodes.values()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        queue.offer(start);

        while (!queue.isEmpty()) {
            TrafficNode current = queue.poll();

            if (current.equals(end)) {
                break;
            }

            for (TrafficEdge edge : current.getOutgoingEdges()) {
                TrafficNode neighbor = edge.getTo();
                double newDist = distances.get(current) + 1;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        // Reconstruct path
        return reconstructPath(previous, end);
    }

    private List<TrafficNode> reconstructPath(Map<TrafficNode, TrafficNode> previous, TrafficNode end) {
        List<TrafficNode> path = new ArrayList<>();
        for (TrafficNode node = end; node != null; node = previous.get(node)) {
            path.add(0, node);
        }
        return path;
    }

    public TrafficNode getNodeAt(Position position) {
        int gridX = position.x / scale;
        int gridY = position.y / scale;

        if (gridX >= 0 && gridX < width && gridY >= 0 && gridY < height) {
            return grid[gridX][gridY];
        }
        return null;
    }

    public TrafficNode getNodeById(String nodeId) {
        return nodes.get(nodeId);
    }

    public TrafficNode getRandomIntersection() {
        List<TrafficNode> intersections = new ArrayList<>();
        for (TrafficNode node : nodes.values()) {
            if (node.getType() == TrafficNode.NodeType.INTERSECTION) {
                intersections.add(node);
            }
        }
        if (!intersections.isEmpty()) {
            return intersections.get(new Random().nextInt(intersections.size()));
        }
        return null;
    }

    // Getters for UI
    public Map<String, TrafficNode> getNodes() { return nodes; }
    public Map<String, TrafficEdge> getEdges() { return edges; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public TrafficNode[][] getGrid() { return grid; }
}
