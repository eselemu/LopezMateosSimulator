package simulation.map;

import java.util.Map;
import java.util.Set;

public class TrafficMap {
    private Map<String, TrafficNode> nodes;
    private Map<String, Set<TrafficNode>> zones; // Para distribución

    // Representación en grid para visualización
    private int width, height;
    private TrafficNode[][] grid;
}
