package simulation;

public class MapManager {
    public static MapManager instance;
    private MapManager() {}
    public static MapManager getInstance() {
        if (instance == null) instance = new MapManager();
        return instance;
    }

}
