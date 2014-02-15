package haven.geoloc;

public class MapTileData {
	public final short weight;
	public final long hash;
	public final short c1;
	public final short c2;
	
	public MapTileData(short weight, long hash, short c1, short c2) {
		this.weight = weight;
		this.hash = hash;
		this.c1 = c1;
		this.c2 = c2;
	}
	
	@Override 
	public String toString() {
		return "w:" + weight + " h:" + hash + " p:("  + c1 + "," + c2 + ")";
	}
}
