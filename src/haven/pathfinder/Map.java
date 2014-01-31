package haven.pathfinder;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import java.util.Random;
import haven.Coord;
import haven.Gob;
import haven.MainFrame;
import haven.MapView;
import haven.Resource;
import haven.Resource.Tile;

public class Map
{
    public int w;
    public int h;
    public int minWeight = Integer.MAX_VALUE;
    public Node nodes[][];
    public int playerSize;  //boat 26, player 4
    public int playerBounds; 
    private static final int VRANGE = 500;
    public static final int MAP_COFFSET_X = 300;
    private static Random rng = new Random();
    
	public Map(int w, int h, int playerSize)
	{
		super();

		this.w = w;
		this.h = h;
		this.playerSize = playerSize;
		this.playerBounds = playerSize/2; // not precise...

		nodes = new Node[w][h];

        for(int i=0;i<h;i++){
            for(int j=0;j<w;j++){
               nodes[j][i] = new Node(j, i);
            }
        }
    }
	
	private void setNode(int x, int y, Node.Type type, boolean isTile) 
	{
		nodes[x][y].type = type;
		nodes[x][y].isTile = isTile;

		if (type.getValue() < minWeight)
			minWeight = type.getValue();
	}
    
	// each pixel considered as a single node
    public void createNodesFromHitbox(int x, int y, int width, int height, Node.Type type, boolean isTile) {
    	for (int i = x; i < x+width; i++) {
	        for (int j = y; j < y+height; j++) {   	
	        	if (nodes[i][j].type != Node.Type.BLOCK && nodes[i][j].type != Node.Type.BLOCK_DYNAMIC)
	        		setNode(i, j, type, isTile);
	        }
    	}
    }
	
    public void initSceneBoat(MapView mv, Gob player, Coord dst, Gob[] gobs) {
        Coord oc = MapView.viewoffsetFloorProjection(MainFrame.getInnerSize(), mv.mc); // offset correction
		Coord playerCoord = player.getc().add(oc).sub(MAP_COFFSET_X, 0);
		
		//playerCoord.add(-12, 13); // boat fix
		
		Coord dstScene = dst.add(oc).sub(MAP_COFFSET_X, 0);
		
        Node src = nodes[playerCoord.x][playerCoord.y];
        Node.srcNode = src;
        
        initTiles(mv, true);
        initGobes(mv, gobs, player, dst);

        // mark all non water tiles as blocked. FIXME: not efficient!
        for(int i=0;i<h;i++){
            for(int j=0;j<w;j++){
               if (nodes[j][i].type != Node.Type.WATER_DEEP &&
            		   nodes[j][i].type != Node.Type.WATER_SHALLOW) {
            	   
            	   if (!(j == src.x && i == src.y ||
            			   dstScene.x == j && dstScene.y == i)) {
            		   nodes[j][i].type = Node.Type.BLOCK;
            	   }
               }
            }
        }
        
        initClearances();
    }
    
    public void initScene(MapView mv, Gob player, Coord dst, Gob[] gobs) {
		// setup player current location
        Coord oc = MapView.viewoffsetFloorProjection(MainFrame.getInnerSize(), mv.mc); // offset correction
		Coord playerCoord = player.getc().add(oc).sub(MAP_COFFSET_X, 0);
		
        Node src = nodes[playerCoord.x][playerCoord.y];
        Node.srcNode = src;
        
        initTiles(mv, false);
        initGobes(mv, gobs, player, dst);
        initClearances();
    }
    
    private void initTiles(MapView mv, boolean waterFix) {
		Coord frameSz = MainFrame.getInnerSize();
		Coord oc = MapView.viewoffsetFloorProjection(frameSz, mv.mc); // offset correction
		
		Coord requl = mv.mc.add(-VRANGE, -VRANGE).div(tilesz).div(cmaps);
		Coord reqbr = mv.mc.add(VRANGE, VRANGE).div(tilesz).div(cmaps);
				
		Coord cgc = new Coord(0, 0);
		for(cgc.y = requl.y; cgc.y <= reqbr.y; cgc.y++) {
		    for(cgc.x = requl.x; cgc.x <= reqbr.x; cgc.x++) {
			if(mv.map.grids.get(cgc) == null)
				mv.map.request(new Coord(cgc));
		    }
		}
		
		Coord tc = mv.mc.div(tilesz);
	
		for(int y = -VRANGE/tilesz.y; y < VRANGE/tilesz.y; y++) {
		    for(int x = -VRANGE/tilesz.x; x < VRANGE/tilesz.x; x++) {
			    	
			    Coord ctc = tc.add(new Coord(x, y));
			    Coord sc = ctc.mul(tilesz).add(oc);
			    
			    sc.x -= MAP_COFFSET_X;

			    if (sc.x+tilesz.x >= w || sc.y+tilesz.y >= h || sc.x < 0 || sc.y < 0 )
			    	continue;

			    Tile groundTile = mv.map.getground(ctc);
			    Node.Type tileType = groundTile.resolveTileType();
			    	
			    if (groundTile != null) {
		        	if (tileType == Node.Type.NOT_IMPLEMENTED) {
		        		System.out.format("TILE: %s   %s  XxY:%sx%s   ctc:%s   sc:%s\n", groundTile.getOuter().name, tileType, x, y, ctc, sc);
		        	}
		        	
		        	if (tileType != Node.Type.IGNORE) {
		        		int width = tilesz.x;
		        		int height = tilesz.x;
		        		if (waterFix) { //FIXME: probably can be removed now...
		        			width += tilesz.x+2 > w ? 0 : 2;
		        			height += tilesz.x+2 > h ? 0 : 2;
		        		}
		        		createNodesFromHitbox(sc.x, sc.y, width, height, tileType, true);
		        	}
			    }
		    }
		}
    }
    
    private void initGobes(MapView mv, Gob[] gobs, Gob player, Coord dst) {
		Coord frameSz = MainFrame.getInnerSize();
		Coord oc = MapView.viewoffsetFloorProjection(frameSz, mv.mc); // offset correction
        
		Coord playerCoord = player.getc().add(oc).add(-MAP_COFFSET_X, 0);
		
    	for (Gob g : gobs) {
    	    Node.Type t = g.resolveObType(mv);
    	    
        	Resource.Neg neg =  g.getneg();
        	if (neg == null)
        		continue;
        	
        	// NOTE: for objects with hitbox -  bs.x and bs.y > 0 but not for all. e.g. straw bed.        	

            Coord a =  g.getc().add(neg.bc).add(oc).add(-MAP_COFFSET_X, 0);
            Coord c =  g.getc().add(neg.bc).add(neg.bs).add(oc).add(-MAP_COFFSET_X, 0);
            
        	if (t == Node.Type.NOT_IMPLEMENTED) {
        		System.out.format("[NEG] cc:%s   bs:%s   bc:%s   sz:%s\n", neg.cc.toString(), neg.bs.toString(), neg.bc.toString(), neg.sz.toString());
        		System.out.format("[GOB] getc():%s   sc:%s     %s\n", g.getc(), g.sc != null ? g.sc.toString() : "null", g.getres() != null? g.getres().name : "res is null");
        		System.out.format("[HB] a:%s   c:%s\n", a, c); 
        	}

        	// so we don't block ourself with gear gobs, boats, etc.
    		if (player.getc().equals(g.getc()) ||
    				playerCoord.x <= c.x && playerCoord.x >= a.x &&
    				playerCoord.y <= c.y && playerCoord.y >= a.y) // sometimes obj pos lags behind so it's not exactly same coord
    			continue;
        	
    		// make sure the destination is not blocked by gob hitbox
    		if (dst != null && dst.equals(g.getc())) {
    			continue;
    		}
        	if (t != Node.Type.IGNORE &&
        			a.x + (c.x-a.x) < w && a.y + (c.y-a.y) < h &&
        			a.x >= 0 && a.y >= 0 && c.x >= 0 && c.y >= 0) {
        		
        		// NOTE: since we calculate clearances by expanding East-South
        		// we need to account for upper and left player hitbox bounds
        		int width = c.x+playerBounds+1 >= w ? w-a.x : c.x-a.x+playerBounds+1;
        		int height = c.y+playerBounds+1 >= h ? h-a.y : c.y-a.y+playerBounds+1;
        		createNodesFromHitbox(a.x, a.y, width, height, t, false);
        	}
    	}
    }  
    
    // Initialize clearance values for an object with bound playerBounds (player = 4/2+1, boat = 26/2+1).
    // Final node clearance will be either NO_CLEARANCE or 'playerBounds'. 
    //
    // NOTE: Nodes with clearance=NO_CLEARANCE could be actually traversed by objects of size < 'playerBounds'
    //       hence, the clearance values need to be recomputed each time for objects of different size.
    private void initClearances() {
    	for (int x = w - 1; x >= 0; x--) {
    		for (int y = h - 1; y >= 0; y--) {

    			if (x+playerBounds >= w || y+playerBounds >= h) {
    				nodes[x][y].clearance = 1;	
    			} else {    				
    				boolean allClear = true;
    				int i;
    				for (i = 0; i < playerBounds; i++) {
    	    			if (nodes[x+i][y].type == Node.Type.BLOCK || nodes[x+i][y].type == Node.Type.BLOCK_DYNAMIC ||
    	    	    			nodes[x][y+i].type == Node.Type.BLOCK || nodes[x][y+i].type == Node.Type.BLOCK_DYNAMIC ||
    	    	    			nodes[x+i][y+i].type == Node.Type.BLOCK || nodes[x+i][y+i].type == Node.Type.BLOCK_DYNAMIC) {
    	    				allClear = false;
    	    				break;
    	    			}    					
    				}
    		
    				nodes[x][y].clearance = allClear ? playerBounds : i-1;
    			}
    		}
    	}
    }
    
    public Node[] getAdjacent4(Node n){
        Node next[] = new Node[4];
        
        // top
        if(n.y > 0 && !nodes[n.x][n.y-1].pathTraversed) {
        	next[0] = nodes[n.x][n.y-1];   
        	next[0].parent = n;
        }
        // right
        if(n.x+1 < w && !nodes[n.x+1][n.y].pathTraversed) {
        	next[1] = nodes[n.x+1][n.y];
        	next[1].parent = n;
        }
        // bottom
        if(n.y+1 < h && !nodes[n.x][n.y+1].pathTraversed) {
        	next[2] = nodes[n.x][n.y+1];
        	next[2].parent = n;
        }
        // left
        if(n.x > 0 && !nodes[n.x-1][n.y].pathTraversed) {
        	next[3] = nodes[n.x-1][n.y];
        	next[3].parent = n;
        }

        return next;
    }
    
    
    public Node[] getAdjacent8(Node n){
        Node next[] = new Node[8];
        
        // top
        if(n.y > 0 && !nodes[n.x][n.y-1].pathTraversed) {
        	next[0] = nodes[n.x][n.y-1];   
        	next[0].parent = n;
        }
        // right
        if(n.x+1 < w && !nodes[n.x+1][n.y].pathTraversed) {
        	next[1] = nodes[n.x+1][n.y];
        	next[1].parent = n;
        }
        // bottom
        if(n.y+1 < h && !nodes[n.x][n.y+1].pathTraversed) {
        	next[2] = nodes[n.x][n.y+1];
        	next[2].parent = n;
        }
        // left
        if(n.x > 0 && !nodes[n.x-1][n.y].pathTraversed) {
        	next[3] = nodes[n.x-1][n.y];
        	next[3].parent = n;
        }
        // WN
        if(n.y > 0 && n.x > 0 && !nodes[n.x-1][n.y-1].pathTraversed) {
        	next[4] = nodes[n.x-1][n.y-1];
        	next[4].parent = n;
        }
        // NE
        if(n.y > 0 && n.x+1 < w && !nodes[n.x+1][n.y-1].pathTraversed) {
        	next[5] = nodes[n.x+1][n.y-1];
        	next[5].parent = n;
        }
        // ES
        if(n.y+1 < h && n.x+1 < w && !nodes[n.x+1][n.y+1].pathTraversed) {
        	next[6] = nodes[n.x+1][n.y+1]; 
        	next[6].parent = n;
        }
        // SW
        if(n.y+1 < h && n.x > 0 && !nodes[n.x-1][n.y+1].pathTraversed){
        	next[7] = nodes[n.x-1][n.y+1];   
        	next[7].parent = n;
        }
        
        return next;
    }
    
    public Coord findLandMark(Coord currentPos, int distanceToIgnore) {
    	
    	int X = w-1;
    	int Y = h-1;
    	
    	int x=currentPos.x, y=currentPos.y, dx = 0, dy = -1;
	    int t = Math.max(X,Y);
	    int maxI = t*t;

	    for (int i=0; i < maxI; i++){
	        if ((-X/2 <= x) && (x <= X/2) && (-Y/2 <= y) && (y <= Y/2)) {
	            System.out.println(x+","+y);
	            
        		if (x+tilesz.x < w &&
    					nodes[x][y].type == Node.Type.PAVEMENT &&
    					nodes[x+tilesz.x][y].type == Node.Type.PAVEMENT) {
        			return new Coord(x, y);
        		}
	        }

	        if( (x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1-y))) {
	            t=dx; dx=-dy; dy=t;
	        }   
	        x+=dx; y+=dy;
	    }
    	
    	

    	return null;
    }
    
    public boolean isEnoughSpace(int xp, int yp, int objSize) {
		for (int x = xp - objSize/2; x < xp+objSize/2; x++) {
		    for (int y = yp - objSize/2; y < yp+objSize/2; y++) {
        		if (x >= w || y >= h || x < 0 || y < 0 || 
    					nodes[x][y].type == Node.Type.WATER_DEEP || 
    					nodes[x][y].type == Node.Type.WATER_SHALLOW || 
    					nodes[x][y].type == Node.Type.BLOCK ||  
    					nodes[x][y].type == Node.Type.BLOCK_DYNAMIC) {
        			return false;
        		}
		    }
		}
    	return true;
    }
    
    //FIXME not efficient!
    public Coord findEmptyGroundTile(Coord currentPos, int objSize) {
    	Coord closest =  new Coord(0,0);
    	
    	for (int x = currentPos.x+playerSize+tilesz.x; x < currentPos.x+playerSize+50*tilesz.x; x+=tilesz.x) {
        	for (int y = currentPos.y+playerSize+tilesz.y; y < currentPos.y+playerSize+50*tilesz.y; y+=tilesz.y) {
        		
        		if (x+1 < w && y+1 < h &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			
        			if (isEnoughSpace(x, y, objSize) && new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}
    	
    	for (int x = currentPos.x+playerSize+tilesz.x; x < currentPos.x+playerSize+50*tilesz.x; x+=tilesz.x) {
        	for (int y = currentPos.y-playerSize-+tilesz.y; y > currentPos.y-playerSize+50*tilesz.y; y-=tilesz.y) {
        		
        		if (x+1 < w && y >= 0 &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {

        			if (isEnoughSpace(x, y, objSize) && new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}
    	
    	for (int x = currentPos.x-playerSize-tilesz.x; x > currentPos.x-playerSize-50*tilesz.x; x-=tilesz.x) {
        	for (int y = currentPos.y+playerSize+tilesz.y; y < currentPos.y+playerSize+50*tilesz.y; y+=tilesz.y) {
        		
        		if (x >= 0 && y+1 < h &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			
        			if (isEnoughSpace(x, y, objSize) && new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}

    	for (int x = currentPos.x-playerSize-tilesz.x; x > currentPos.x-playerSize-50*tilesz.x; x-=tilesz.x) {
        	for (int y = currentPos.y-playerSize-tilesz.y; y > currentPos.y-playerSize-50*tilesz.x; y-=tilesz.y) {

        		if (x >= 0 && y >= 0 &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			
        			if (isEnoughSpace(x, y, objSize) && new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}
    	
    	return closest;
    }
    
    // find random water tile N tiles away from the given position
    public Coord findRandomWaterTile(Coord currentPos) {

    	int DIST_FROM_CUR = 33*tilesz.x;
    	int BORDER_LIMIT = 3*tilesz.x;

    	for (int i = 0; i < 5000; i++) {
	    	int xlow, xhigh;
	    	if (rng.nextBoolean()) {
	        	xlow = BORDER_LIMIT;
	            xhigh = currentPos.x-DIST_FROM_CUR;
	    	} else {
	        	xlow = currentPos.x+DIST_FROM_CUR;
	            xhigh = w - BORDER_LIMIT;
	    	}
	    	
	    	if (xhigh-xlow < 1) 
	    		continue;
	    	
	    	int xr = rng.nextInt(xhigh-xlow)+xlow;
	    	
	    	int ylow, yhigh;
	    	if (rng.nextBoolean()) {
	        	ylow = currentPos.y+DIST_FROM_CUR; 
	            yhigh = h - BORDER_LIMIT;
	    	} else {
	        	ylow = BORDER_LIMIT;
	            yhigh = currentPos.y-DIST_FROM_CUR;
	    	}
	    	
	    	if (yhigh-ylow < 1) 
	    		continue;
	    	
	    	int yr = rng.nextInt(yhigh-ylow)+ylow;

			if (nodes[xr][yr].type == Node.Type.WATER_DEEP) {
				return new Coord(xr, yr);
			}
    	}

    	return null;
    }
    
    // find a shallow tile at the defined DISTANCE.
    public Coord findNextShallowTile(Coord currentPos, Coord prevPos) {
    	int DISTANCE = 10*tilesz.x;

    	// ugly fix for avoiding getting stuck at small islands inside a lake
    	int range = (3 - 1) + 1;    // [1,3] 
    	DISTANCE += ((int)(Math.random() * range) + 1)*tilesz.x;

    	int cornerX = currentPos.x-DISTANCE;	
    	int cornerY = currentPos.y-DISTANCE;

    	for (int i = 0; i <= DISTANCE*2; i+=tilesz.x) {
    		// x top
    		Coord tilePos = nextShallowTileCheck(DISTANCE, currentPos, prevPos, cornerX+i, currentPos.y-DISTANCE);
    		if (tilePos != null)
    			return tilePos;
    		// x bottom
    		tilePos = nextShallowTileCheck(DISTANCE, currentPos, prevPos, cornerX+i, currentPos.y+DISTANCE);
    		if (tilePos != null)
    			return tilePos;
    		// y left
    		tilePos = nextShallowTileCheck(DISTANCE, currentPos, prevPos, currentPos.x-DISTANCE, cornerY+i);
    		if (tilePos != null)
    			return tilePos;
    		// y right
    		tilePos = nextShallowTileCheck(DISTANCE, currentPos, prevPos, currentPos.x+DISTANCE, cornerY+i);
    		if (tilePos != null)
    			return tilePos;
    	}

		return null;
    }
    
    
    public Coord findClosesWaterFromShore(Coord currentPos) {
    	
    	for (int DISTANCE = tilesz.x; DISTANCE < tilesz.x*10; DISTANCE+=tilesz.x) {
    		
        	int cornerX = currentPos.x-DISTANCE;	
        	int cornerY = currentPos.y-DISTANCE;
    		
	    	for (int i = tilesz.x/2; i <= DISTANCE*2; i+=tilesz.x) {
	    		// x top
	    		if (isWaterTileCloseToShore(cornerX+i, currentPos.y-DISTANCE))
	    			return new Coord(cornerX+i, currentPos.y-DISTANCE);
	    		// x bottom
	    		if (isWaterTileCloseToShore(cornerX+i, currentPos.y+DISTANCE))
	    			return new Coord(cornerX+i, currentPos.y+DISTANCE);
	    		// y left
	    		if (isWaterTileCloseToShore(currentPos.x-DISTANCE, cornerY+i))
	    			return new Coord(currentPos.x-DISTANCE, cornerY+i);
	    		// y right
	    			if (isWaterTileCloseToShore(currentPos.x+DISTANCE, cornerY+i))
	    			return new Coord(currentPos.x+DISTANCE, cornerY+i);
	    	}
    	}
    	
    	return null;
    }
    
    private boolean isWaterTileCloseToShore(int x, int y) {

		if (x-tilesz.x > 0 && y-tilesz.x > 0 && x+tilesz.x < w && y+tilesz.x < h &&
				nodes[x][y].type == Node.Type.WATER_SHALLOW) {
		
			// follow only outer shallow waters
			if (nodes[x+tilesz.x][y].type != Node.Type.WATER_DEEP && nodes[x+tilesz.x][y].type != Node.Type.WATER_SHALLOW ||
					nodes[x][y+tilesz.x].type != Node.Type.WATER_DEEP && nodes[x][y+tilesz.x].type != Node.Type.WATER_SHALLOW ||
    				nodes[x-tilesz.x][y].type != Node.Type.WATER_DEEP && nodes[x-tilesz.x][y].type != Node.Type.WATER_SHALLOW ||
    				nodes[x][y-tilesz.x].type != Node.Type.WATER_DEEP && nodes[x][y-tilesz.x].type != Node.Type.WATER_SHALLOW) {
				return true;
			} 
		}
		
		return false;
    }
    
    
    private Coord nextShallowTileCheck(int distance, Coord currentPos, Coord prevPos, int x, int y) {
		Coord newPos = new Coord(x, y);

		if (x-tilesz.x > 0 && y-tilesz.x > 0 && x+tilesz.x < w && y+tilesz.x < h &&
				nodes[x][y].type == Node.Type.WATER_SHALLOW &&
				newPos.dist(prevPos) >= newPos.dist(currentPos)) {
		
			// follow only outer shallow waters
			if (nodes[x+tilesz.x][y].type == Node.Type.WATER_DEEP ||
					nodes[x][y+tilesz.x].type == Node.Type.WATER_DEEP ||
    				nodes[x-tilesz.x][y].type == Node.Type.WATER_DEEP ||
    				nodes[x][y-tilesz.x].type == Node.Type.WATER_DEEP) {
				return newPos;
			} 
		}
		
		return null;
    }
    
    public Coord findClosestShoreTile(Coord currentPos) {
    	Coord closest =  new Coord(0,0);
    	
    	for (int x = currentPos.x+playerSize; x < currentPos.x+playerSize+50*tilesz.x; x+=tilesz.x) {
        	for (int y = currentPos.y+playerSize; y < currentPos.y+playerSize+50*tilesz.y; y+=tilesz.y) {
        		
        		if (x+1 < w && y+1 < h &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			
        			if (new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}

    	for (int x = currentPos.x+playerSize; x < currentPos.x+playerSize+50*tilesz.x; x+=tilesz.x) {
        	for (int y = currentPos.y-playerSize; y > currentPos.y-playerSize+50*tilesz.y; y-=tilesz.y) {
        		
        		if (x+1 < w && y >= 0 &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			if (new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}
    	
    	for (int x = currentPos.x-playerSize; x > currentPos.x-playerSize-50*tilesz.x; x-=tilesz.x) {
        	for (int y = currentPos.y+playerSize; y < currentPos.y+playerSize+50*tilesz.y; y+=tilesz.y) {
        		
        		if (x >= 0 && y+1 < h &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			if (new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}

    	for (int x = currentPos.x-playerSize; x > currentPos.x-playerSize-50*tilesz.x; x-=tilesz.x) {
        	for (int y = currentPos.y-playerSize; y > currentPos.y-playerSize-50*tilesz.x; y-=tilesz.y) {

        		if (x >= 0 && y >= 0 &&
    					nodes[x][y].type != Node.Type.WATER_DEEP &&
    					nodes[x][y].type != Node.Type.WATER_SHALLOW &&
    					nodes[x][y].type != Node.Type.BLOCK && 
    					nodes[x][y].type != Node.Type.BLOCK_DYNAMIC) {
        			if (new Coord(x, y).dist(currentPos) < closest.dist(currentPos))
        				closest = new Coord(x, y);
        		}
        	}
    	}
    	
    	return closest;
    }
}