package haven.geoloc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;


public class Geoloc {
	private static final int IMG_W = 100;
	private static final int IMG_H = 100;

	
	public static BufferedImage preprocessMapTile(BufferedImage img) {
		Image imgStripped = stripEverythingButBlue(img);
		return imageToBufferedImage(imgStripped, IMG_W, IMG_H);
	}

	public static BufferedImage imageToBufferedImage(Image image, int width, int height) {
		BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = dest.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();
		return dest;
	}
	
	public static MapTileData getHash(BufferedImage img) {
		WritableRaster raster = img.getRaster();
		DataBufferInt dataBuffer = (DataBufferInt)raster.getDataBuffer();
		int[] data = dataBuffer.getData();
		
		short weight = 0;
		for (int i = 0; i < data.length; i++) {
			if ((data[i] & 0xFFFFFF) != 0xFFFFFF) {
				weight++;
			}
		}

	    PHash ph = new PHash(8, 8);
	    long hash = ph.getHash(img);

	    return new MapTileData(weight, hash, (short)0, (short)0);
	}
	
	public static int hammingDistance(String s1, String s2) {
	    int d = 0;

	    if (s1.length() != s2.length())
	        return -1; 
	
	    for (int i = 0; i < s1.length(); i++) {
	        if (s1.charAt(i) != s2.charAt(i))
	            d++;
	    }
	    
	    return d;
	}
	
	public static int hammingDistance(long x, long y) {
	    int dist = 0;
	    long val = x ^ y;
	    
	    while(val != 0) {
	    	dist++;
	    	val &= val - 1;
	    }
	    return dist;
    }
	
	public static Image stripEverythingButBlue(BufferedImage image) {
		ImageFilter filter = new RGBImageFilter() {
			public int filterRGB(int x, int y, int rgb) {
				float MIN_BLUE_HUE = 0.5f; // CYAN
				float MAX_BLUE_HUE = 0.8333333f; // MAGENTA
				float UNMAPPED_HUE = 0.6666667f;
				
				int r = (rgb & 0x00ff0000) >> 0x10;
				int g = (rgb & 0x0000ff00) >> 0x08;
				int b = (rgb & 0x000000ff);
				float[] hsv = new float[3];
				Color.RGBtoHSB(r, g, b, hsv);

				if (hsv[0] < MIN_BLUE_HUE || hsv[0] > MAX_BLUE_HUE || hsv[0] == UNMAPPED_HUE) {
					return Color.WHITE.getRGB();
				} else {
					return rgb;
				}
			}
		};

		ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}
}
