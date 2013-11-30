package haven.pathfinder;

import haven.Coord;

import java.util.*;


// A* with Manhattan distance heuristic.
// 
// TODO: clearance support.
// TODO: diagonal movement with Chebyshev distance heuristic.

public class AStar implements PathFinder
{
    protected ArrayList<Node> open;
    protected ArrayList<Node> closed;


    protected double mode; 
    private static final int INITIAL_CAPACITY = 100;
    
    public AStar() {
        super();
    }

    public boolean find(Map map, Coord destination, boolean isFast)
    {
    	Node.dstNode = map.cells[destination.x][destination.y];
    	
    	mode = isFast?2:map.minWeight;

        open = new ArrayList<Node>(INITIAL_CAPACITY);
        open.add(Node.getSrc());
        
        closed = new ArrayList<Node>(INITIAL_CAPACITY);

        boolean found = false;

        while(!found) {
        	
            Node dst = Node.getDst();
            double min = Double.MAX_VALUE;

            Node best = (Node)open.get(open.size() - 1);
            Node now;
            for(int i = 0; i < open.size(); i++) {
                now = (Node)open.get(i);
                if(!closed.contains(now)) {
                	double score = now.distFromSrc();
                    score += manhattanDist(now.x, now.y, dst.x, dst.y, mode);
                    if(score < min) {
                        min = score;
                        best = now;
                    }
                }
            }
            
            now = best;
            open.remove(now);
            closed.add(now);
            
            Node next[] = map.getAdjacent4(now);
            
            for(int i = 0; i < 4; i++){
                if(next[i] != null) {
                    if(next[i].type != Node.Type.BLOCK && next[i].type != Node.Type.BLOCK_DYNAMIC) {
                        next[i].addToPathFromStart(now.distFromSrc());
                        if(!open.contains(next[i]) && !closed.contains(next[i]))
                        	open.add(next[i]);
                    }
                    if(next[i] == dst) {
                    	found = true;
                    	break;
                    }
                }
            }

            now.pathTraversed = true;
            
            if(open.isEmpty())
            	break;
        }
        
        // if path has been found mark all the nodes within it
        if(found) {
            Node next;
            Node cur = Node.getDst();
            Node end = Node.getSrc();
            while(cur != end) {
                next = map.getLowestAdjacent4(cur);
                cur = next;
                cur.setPartOfPath(true);
            }
            return true;
        }
        
        return false;
    }
    

    
    public double manhattanDist(int ax, int ay, int bx, int by, double d) {
        return d * (Math.abs(ax - bx) + Math.abs(ay - by));
    }
    
    public double chebyshevDist(int ax, int ay, int bx, int by, double d) {
        return d * Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }
}