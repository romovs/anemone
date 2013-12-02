package haven.pathfinder;

import haven.Coord;

import java.util.*;


// A* with Manhattan/Chebyshev distance heuristics.
//
public class AStar implements PathFinder
{
    protected ArrayList<Node> open;
    protected HashSet<Node> closed;
    
    private static final int MAX_ITERATIONS = 100000;

    protected int mode; 
    private static final int INITIAL_CAPACITY = 100;
    private static final int PATH_CAPACITY = 100;
    private static final int FAST_MODE_D = 3;
    
    public AStar() {
        super();
    }

    public List<Node> find(Map map, Coord destination, boolean isFast)
    {
    	Node.dstNode = map.nodes[destination.x][destination.y];
    	
    	mode = isFast?FAST_MODE_D:map.minWeight;

        open = new ArrayList<Node>(INITIAL_CAPACITY);
        open.add(Node.getSrc());
        closed = new HashSet<Node>();

        boolean found = false;

        int iter = 0;
        while(iter < MAX_ITERATIONS && !found) {
        	iter++;
            Node dst = Node.getDst();
            double min = Double.MAX_VALUE;

            Node best = (Node)open.get(open.size() - 1);
            Node now;
            for(int i = 0; i < open.size(); i++) {
                now = (Node)open.get(i);
                if(!closed.contains(now)) {
                	double f = now.distFromSrc() + distChebyshev(now.x, now.y, dst.x, dst.y, mode);
                    if(f < min) {
                        min = f;
                        best = now;
                    }
                }
            }
            
            now = best;
            open.remove(now);
            closed.add(now);
            
            Node next[] = map.getAdjacent8(now);
            
            for(int i = 0; i < 8; i++){
            	Node nxt = next[i];
                if(nxt != null) {
                    if(nxt.type != Node.Type.BLOCK && nxt.type != Node.Type.BLOCK_DYNAMIC &&
                    		nxt.clearance > Map.NO_CLEARANCE) {
                        nxt.addToPathFromSrc(now.distFromSrc()); 
                        nxt.pathTraversed = true;
                        if(!open.contains(nxt) && !closed.contains(nxt))
                        	open.add(nxt);
                    }
                    if(nxt == dst) {
                    	found = true;
                    	break;
                    }
                }
            }
        }
        
        // if path has been found mark all the nodes within it
        if(found) {
            List<Node> path = new ArrayList<Node>(PATH_CAPACITY);
            Node cur = Node.getDst();
            Node end = Node.getSrc();
            while(cur != end) {
            	path.add(cur);
                cur.addToPathFromDst(cur.distFromDst());
                cur = cur.parent;
                cur.setPartOfPath(true);
            }
            Collections.reverse(path);
            return path;
        }
        
        return null;
    }
    
    public int distManhattan(int ax, int ay, int bx, int by, int d) {
        return d * (Math.abs(ax - bx) + Math.abs(ay - by));
    }
    
    public int distChebyshev(int ax, int ay, int bx, int by, int d) {
        return d * Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }
    
    // non-diagonal step is x2 over diagonal
    // http://theory.stanford.edu/~amitp/GameProgramming/Heuristics.html
    public int distLowerDiagonal(int ax, int ay, int bx, int by, int d, int d2) {
        int dx = Math.abs(ax - bx);
        int dy = Math.abs(ay - by);
        return d * (dx + dy) + (d2 - 2 * d) * Math.min(dx, dy);
    }
}