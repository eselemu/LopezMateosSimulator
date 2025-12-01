package simulation.map;

import simulation.agents.SemaphoreSimulation;
import java.util.*;

public class TrafficMap {
    private Map<String, TrafficNode> nodes;
    private Map<String, TrafficEdge> edges;

    // Representación en grid para visualización
    private int width, height;
    private TrafficNode[][] grid;

    private int scale = 50;

    public TrafficMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
        this.grid = new TrafficNode[width][height];
        initializeGridMap();
    }

    private void initializeGridMap() {
        boolean toggle = true;
        // Create nodes in a grid pattern
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TrafficNode.NodeType type = determineNodeType(x, y);
                String nodeId = "N_" + x + "_" + y;
                Position pos = new Position(x * scale, y * scale);

                TrafficNode node = new TrafficNode(nodeId, pos, type);
                nodes.put(nodeId, node);
                grid[x][y] = node;

                // Add semaphore at intersections (only some intersections)
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

        createUnidirectionalGridEdges();
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

    private void createUnidirectionalGridEdges() {
        // Create unidirectional edges following the pattern:
        // Even rows: left → right
        // Odd rows: right → left
        // Even columns: top → bottom
        // Odd columns: bottom → top

        // Horizontal edges (rows)
        for (int y = 0; y < height; y += 2) { // Only intersection rows
            if (y % 4 == 0) { // Even rows: left → right
                for (int x = 0; x < width - 2; x += 2) {
                    createUnidirectionalHorizontalEdge(x, y, x + 2, y, "E");
                }
            } else { // Odd rows: right → left
                for (int x = width - 1; x >= 2; x -= 2) {
                    createUnidirectionalHorizontalEdge(x, y, x - 2, y, "W");
                }
            }
        }

        // Vertical edges (columns)
        for (int x = 0; x < width; x += 2) { // Only intersection columns
            if (x % 4 == 0) { // Even columns: top → bottom
                for (int y = 0; y < height - 2; y += 2) {
                    createUnidirectionalVerticalEdge(x, y, x, y + 2, "S");
                }
            } else { // Odd columns: bottom → top
                for (int y = height - 1; y >= 2; y -= 2) {
                    createUnidirectionalVerticalEdge(x, y, x, y - 2, "N");
                }
            }
        }
    }

    private void createUnidirectionalHorizontalEdge(int fromX, int fromY, int toX, int toY, String direction) {
        TrafficNode from = grid[fromX][fromY];
        TrafficNode to = grid[toX][toY];
        TrafficNode streetNode = grid[(fromX + toX) / 2][fromY]; // Intermediate street node

        if (from != null && to != null && streetNode != null) {
            String baseId = direction + "_" + fromX + "_" + fromY + "_to_" + toX + "_" + toY;

            // Edge from intersection to street
            TrafficEdge edge1 = new TrafficEdge(baseId + "_1", from, streetNode, scale);
            edges.put(edge1.getEdgeId(), edge1);
            from.addOutgoingEdge(edge1);
            streetNode.addIncomingEdge(edge1);

            // Edge from street to intersection
            TrafficEdge edge2 = new TrafficEdge(baseId + "_2", streetNode, to, scale);
            edges.put(edge2.getEdgeId(), edge2);
            streetNode.addOutgoingEdge(edge2);
            to.addIncomingEdge(edge2);

            System.out.println("Created horizontal edge: " + from.nodeId + " → " + to.nodeId + " (" + direction + ")");
        }
    }

    private void createUnidirectionalVerticalEdge(int fromX, int fromY, int toX, int toY, String direction) {
        TrafficNode from = grid[fromX][fromY];
        TrafficNode to = grid[toX][toY];
        TrafficNode streetNode = grid[fromX][(fromY + toY) / 2]; // Intermediate street node

        if (from != null && to != null && streetNode != null) {
            String baseId = direction + "_" + fromX + "_" + fromY + "_to_" + toX + "_" + toY;

            // Edge from intersection to street
            TrafficEdge edge1 = new TrafficEdge(baseId + "_1", from, streetNode, scale);
            edges.put(edge1.getEdgeId(), edge1);
            from.addOutgoingEdge(edge1);
            streetNode.addIncomingEdge(edge1);

            // Edge from street to intersection
            TrafficEdge edge2 = new TrafficEdge(baseId + "_2", streetNode, to, scale);
            edges.put(edge2.getEdgeId(), edge2);
            streetNode.addOutgoingEdge(edge2);
            to.addIncomingEdge(edge2);

            System.out.println("Created vertical edge: " + from.nodeId + " → " + to.nodeId + " (" + direction + ")");
        }
    }

    /**
     * Enhanced Dijkstra's algorithm for unidirectional graph
     */
    public List<TrafficNode> findShortestPath(TrafficNode start, TrafficNode end) {
        if (start == null || end == null) {
            System.out.println("Invalid start or end node");
            return Collections.emptyList();
        }

        if (start.equals(end)) {
            System.out.println("Start and end are the same");
            return Arrays.asList(start);
        }

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

            // Explore all outgoing edges (unidirectional graph)
            for (TrafficEdge edge : current.getOutgoingEdges()) {
                TrafficNode neighbor = edge.getTo();
                double newDist = distances.get(current) + edge.getLength();

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        // Check if path exists
        if (!previous.containsKey(end)) {
            System.out.println("No path exists from " + start.nodeId + " to " + end.nodeId);
            return Collections.emptyList();
        }

        // Reconstruct path
        List<TrafficNode> path = reconstructPath(previous, end);
        System.out.println("Path found: " + path.size() + " nodes from " + start.nodeId + " to " + end.nodeId);
        return path;
    }

    private List<TrafficNode> reconstructPath(Map<TrafficNode, TrafficNode> previous, TrafficNode end) {
        List<TrafficNode> path = new ArrayList<>();
        for (TrafficNode node = end; node != null; node = previous.get(node)) {
            path.add(0, node);
        }
        return path;
    }

    /**
     * Get valid start nodes (only those with outgoing edges)
     */
    public List<TrafficNode> getValidStartNodes() {
        List<TrafficNode> startNodes = new ArrayList<>();

        // Only intersections can be start nodes
        for (TrafficNode node : nodes.values()) {
            if (node.getType() == TrafficNode.NodeType.INTERSECTION &&
                    !node.getOutgoingEdges().isEmpty()) {
                startNodes.add(node);
            }
        }

        return startNodes;
    }

    /**
     * Get valid end nodes that are reachable from start node
     */
    public List<TrafficNode> getValidEndNodes(TrafficNode startNode) {
        List<TrafficNode> endNodes = new ArrayList<>();

        if (startNode == null) return endNodes;

        // Use BFS to find all reachable nodes
        Set<TrafficNode> visited = new HashSet<>();
        Queue<TrafficNode> queue = new LinkedList<>();
        queue.offer(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            TrafficNode current = queue.poll();

            // Add as potential end node if it's a different intersection
            if (current.getType() == TrafficNode.NodeType.INTERSECTION &&
                    !current.equals(startNode)) {
                endNodes.add(current);
            }

            for (TrafficEdge edge : current.getOutgoingEdges()) {
                TrafficNode neighbor = edge.getTo();
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        return endNodes;
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

    public TrafficNode getRandomValidStartNode() {
        List<TrafficNode> validStarts = getValidStartNodes();
        if (!validStarts.isEmpty()) {
            return validStarts.get(new Random().nextInt(validStarts.size()));
        }
        return null;
    }

    public TrafficNode getRandomValidEndNode(TrafficNode startNode) {
        List<TrafficNode> validEnds = getValidEndNodes(startNode);
        if (!validEnds.isEmpty()) {
            return validEnds.get(new Random().nextInt(validEnds.size()));
        }
        return null;
    }

    // Getters for UI
    public Map<String, TrafficNode> getNodes() { return nodes; }
    public Map<String, TrafficEdge> getEdges() { return edges; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public TrafficNode[][] getGrid() { return grid; }
    public int getScale() { return scale; }
}