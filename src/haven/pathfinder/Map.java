package haven.pathfinder;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import haven.Coord;
import haven.Gob;
import haven.MCache;
import haven.MainFrame;
import haven.MapView;
import haven.Resource;
import haven.MCache.Grid;
import haven.Resource.Tile;

public class Map
{
    public int w;
    public int h;
    public double minWeight = Double.MAX_VALUE;
    public Node cells[][];
    
	public Map(int w, int h)
	{
		super();

		this.w = w;
		this.h = h;

		cells = new Node[w][h];

        for(int i=0;i<h;i++){
            for(int j=0;j<w;j++){
               cells[j][i] = new Node(j, i);
            }
        }
    }
	
	public void SetCell(int x, int y, Node.Type type, Node srcCell, Node dstCell) 
	{
		cells[x][y].type = type;
		Node.srcNode = srcCell;
		Node.dstNode = dstCell;

		if (type.getValue() < minWeight)
			minWeight = type.getValue();
	}
    
	// each pixel considered as a single node
    public void createNodesFromHitbox(int x, int y, int width, int height, Node.Type type, Node src, Node dst) {
    	for (int i = x; i < x+width; i++) {
	        for (int j = y; j < y+height; j++) {
	    		SetCell(i, j, type, src, dst);
	        }
    	}
    }
	
    public void initScene(MapView mv, Gob player, Gob[] gobs) {
		// setup player current location
        Coord oc = MapView.viewoffsetFloorProjection(MainFrame.getInnerSize(), mv.mc); // offset correction
		Coord playerCoord = player.getc().add(oc);
        Node src = cells[playerCoord.x][playerCoord.y];
        Node.srcNode = src;
        
        initTiles(mv, player);
        initGobes(mv, gobs);
    }
    
    private void initTiles(MapView mv, Gob player) {
    	Node src = Node.srcNode;
    	Node dst = Node.dstNode;
    	MCache map = mv.map;
		Coord frameSz = MainFrame.getInnerSize();
		Coord oc = MapView.viewoffsetFloorProjection(frameSz, mv.mc); // offset correction
		Coord playerCoord = player.getc().add(oc);
        Grid grid = map.last;
		Coord requl = mv.mc.add(-500, -500).div(tilesz).div(cmaps);
		Coord reqbr = mv.mc.add(500, 500).div(tilesz).div(cmaps);
				
		Coord cgc = new Coord(0, 0);
		for(cgc.y = requl.y; cgc.y <= reqbr.y; cgc.y++) {
		    for(cgc.x = requl.x; cgc.x <= reqbr.x; cgc.x++) {
			if(mv.map.grids.get(cgc) == null)
				mv.map.request(new Coord(cgc));
		    }
		}
		
		int stw = (tilesz.x * 4) - 2;
		int sth = tilesz.y * 2;

		Coord tc = mv.mc.div(tilesz);
		tc.x += -(frameSz.x / (2 * stw)) - (frameSz.y / (2 * sth)) - 2;
		tc.y += (frameSz.x / (2 * stw)) - (frameSz.y / (2 * sth));

		for(int y = 0; y < grid.tiles[0].length; y++) {
		    for(int x = 0; x < grid.tiles.length; x++) {
				for(int i = 0; i < 2; i++) {
				    Coord ctc = tc.add(new Coord(x + y, -x + y + i));
				    Coord sc = ctc.mul(tilesz).add(oc);
	
				    if (playerCoord.dist(sc) > 500)
				    	continue;
	
				    Tile groundTile = mv.map.getground(ctc);
				    
				    Node.Type tileType = groundTile.resolveTileType();
				    
				    if (groundTile != null) {
	
					    if (sc.x+11 < w && sc.y+11 < h && sc.x >= 0 && sc.y >= 0) {
							System.out.format("TILE: %s   %s  XxY:%sx%s   ctc:%s   sc:%s\n", groundTile.getOuter().name, tileType, x, y, ctc, sc);
					    	createNodesFromHitbox(sc.x, sc.y, tilesz.x, tilesz.y, tileType, src, dst);
					    }
				    }
				}
		    }
		}
    }
    
    private void initGobes(MapView mv, Gob[] gobs) {
    	Node src = Node.srcNode;
    	Node dst = Node.dstNode;
		Coord frameSz = MainFrame.getInnerSize();
		Coord oc = MapView.viewoffsetFloorProjection(frameSz, mv.mc); // offset correction
        
    	for (Gob g : gobs) {
    	    Node.Type t = g.resolveObType(mv);
    	    
        	Resource.Neg neg =  g.getneg();
        	if (neg == null)
        		continue;
        	
        	// NOTE: for objects with hitbox -  bs.x and bs.y > 0 but not for all. e.g. straw bed.        	
        	System.out.format("[NEG] cc:%s   bs:%s   bc:%s   sz:%s\n", neg.cc.toString(), neg.bs.toString(), neg.bc.toString(), neg.sz.toString());
        	System.out.format("[GOB] getc():%s   sc:%s\n", g.getc(), g.sc.toString());

            Coord a =  g.getc().add(neg.bc).add(oc);
            Coord c =  g.getc().add(neg.bc).add(neg.bs).add(oc);

        	System.out.format("[HB] a:%s   c:%s\n", a, c); 

        	if (a.x + (c.x-a.x) < w && a.y + (c.y-a.y) < h &&
        			a.x >= 0 && a.y >= 0 && c.x >= 0 && c.y >= 0) {
        		createNodesFromHitbox(a.x, a.y, c.x-a.x, c.y-a.y, t, src, dst);
        	}
    	}
    }  
    
    
    public Node[] getAdjacent4(Node n){
        Node next[] = new Node[4];
        
        // top
        if(n.y!=0)
        	next[0]=cells[n.x][n.y-1];        
        // right
        if(n.x!=w-1)
        	next[1]=cells[n.x+1][n.y];
        // bottom
        if(n.y!=h-1)
        	next[2]=cells[n.x][n.y+1];     
        // left
        if(n.x!=0)
        	next[3]=cells[n.x-1][n.y];

        return next;
    }
    
    public Node[] getAdjacent8(Node n){
        Node next[] = new Node[8];
        
        // top
        if(n.y!=0)
        	next[0]=cells[n.x][n.y-1];   
        // right
        if(n.x!=w-1)
        	next[1]=cells[n.x+1][n.y];
        // bottom
        if(n.y!=h-1)
        	next[2]=cells[n.x][n.y+1];
        // left
        if(n.x!=0)
        	next[3]=cells[n.x-1][n.y];
        // WN
        if(n.y!=0 && n.x!=0)
        	next[4]=cells[n.x-1][n.y-1];
        // NE
        if(n.y!=0 && n.x!=w-1)
        	next[5]=cells[n.x+1][n.y-1];
        // ES
        if(n.y!=h-1 && n.x!=w-1)
        	next[6]=cells[n.x+1][n.y+1];        
        // SW
        if(n.y!=h-1 && n.x!=0)
        	next[7]=cells[n.x-1][n.y+1];   
        
        return next;
    }
    
    public Node getLowestAdjacent4(Node n){
        Node next[] = getAdjacent4(n);
        Node lowest = next[0];
        double dist = Double.MAX_VALUE;
        for(int i = 0; i<4; i++) {
            if(next[i]!=null) {
                double nextDist = next[i].distFromSrc();
                if(nextDist < dist && nextDist >= 0) {
                	lowest = next[i];
                    dist = next[i].distFromSrc();
                }
            }
        }
        return lowest;
    }   
    
    public Node getLowestAdjacent8(Node n){
        Node next[] = getAdjacent8(n);
        Node lowest = next[0];
        double dist = Double.MAX_VALUE;
        for(int i = 0; i<8; i++) {
            if(next[i]!=null) {
                double nextDist = next[i].distFromSrc();
                if(nextDist < dist && nextDist >= 0) {
                	lowest = next[i];
                    dist = next[i].distFromSrc();
                }
            }
        }
        return lowest;
    }   
}