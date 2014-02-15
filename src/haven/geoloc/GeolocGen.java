package haven.geoloc;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GeolocGen {
	private static String LOOKUP_FILE = "geoloc.dat";

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: java -jar geolocgen.jar [PATH_TO_ZOOM_9_TILES_DIR]");
			return;
		}
		
		String MAP_DIR = args[0];
		
		DataOutputStream os = null;
		try {
			File lf = new File(LOOKUP_FILE);
			if (!lf.exists())
				lf.createNewFile();

			os = new DataOutputStream(new FileOutputStream(lf.getAbsoluteFile()));

			File[] files = new File(MAP_DIR).listFiles();
			for (File file : files) {
				if (file.isFile()) {
					// ignore wget duplicates (e.g .png.1)
					String fileName = file.getName();
					if (fileName.charAt(fileName.length()-1) != 'g')
						continue;
	
					BufferedImage img = null;
					try {
						img = ImageIO.read(file);
					} catch (IOException e) {
						System.err.println("Cannot read map tile.");
						e.printStackTrace();
					}

					img = Geoloc.preprocessMapTile(img);
					MapTileData mtd = Geoloc.getHash(img);
					
					if (mtd.weight != 0) {
						os.writeShort(mtd.weight);
						os.writeLong(mtd.hash);
						String[] coords = fileName.substring(0, fileName.length() - 4).split("_");
						os.writeShort(Short.valueOf(coords[0]));
						os.writeShort(Short.valueOf(coords[1]));
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to write lookup file.");
			e.printStackTrace();
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}