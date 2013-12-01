package haven.pathfinder;

import java.util.List;
import haven.Coord;


public interface PathFinder
{
    public List<Node> find(Map map, Coord dst, boolean isFast);
}