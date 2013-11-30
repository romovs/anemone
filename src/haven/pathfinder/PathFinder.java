package haven.pathfinder;

import haven.Coord;


public interface PathFinder
{
    public boolean find(Map map, Coord dst, boolean isFast);
}