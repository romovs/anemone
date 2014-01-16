package haven.pathfinder;


public class Node
{
    public enum Type {
    	MOOR(1),
    	GRASS(1),
    	HEATH(1),
    	PAVEMENT(1),
    	MOUNTAIN(1),
    	NORMAL(3),
    	SWAMP(6),
    	THICKET(6),
    	WATER_DEEP(2),
    	WATER_SHALLOW(2),
    	SAND(3),
    	BLOCK(Integer.MAX_VALUE),
    	BLOCK_DYNAMIC(Integer.MAX_VALUE),
    	NOT_IMPLEMENTED(3);
    	
    	private final int w;
	    Type(int w) { this.w = w; }
	    public int getValue() { return w; }
    }
    
    public int x, y;
    public int clearance;
    public static Node srcNode;
    public static Node dstNode;
    public Type type = Type.NORMAL;
    
    public Node parent;
    private long distFromSrc = -1;
    private long distFromDst = -1;

    private boolean partOfPath = false;
    public boolean pathTraversed = false;


    public Node() {
    }
    
    public Node(int x, int y) {
    	this.x = x;
    	this.y = y;
    }
    
    public void addToPathFromSrc(long distSoFar){
    	distFromSrc = distSoFar + type.getValue();
    }
    
    public void addToPathFromDst(long distSoFar) {
    	distFromDst = distSoFar + type.getValue();
    }
    
    public static Node getSrc() {
        return srcNode;
    }
    
    public static Node getDst() {
        return dstNode;
    }
    
    public boolean isSrc() {
        return srcNode == this;
    }
    
    public boolean isDst() {
        return dstNode == this;
    }
    
    public boolean isPartOfPath() {
        return partOfPath;
    }
    
    public void setPartOfPath(boolean isInPath) {
        partOfPath = isInPath;
    }
   
    public long distFromSrc() {
        if(Node.srcNode == this)
        	return 0;
        if(type == Node.Type.BLOCK || type == Node.Type.BLOCK_DYNAMIC)
        	return -1;
        
        return distFromSrc;
    }
    
    public long distFromDst() {
        if(Node.dstNode == this)
        	return 0;
        if(type == Node.Type.BLOCK || type == Node.Type.BLOCK_DYNAMIC)
        	return -1;
        
        return distFromDst;
    }
    
    @Override
    public String toString() {
    	return "(" + x + "," + y + ")";
    }
}