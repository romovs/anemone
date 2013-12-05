/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;

import addons.MainScript; // new

@SuppressWarnings("serial")
public class MainFrame extends Frame implements FSMan {
    private static final String TITLE = String.format("Haven and Hearth (Anemone v%s)", Version.VERSION);
    public static HavenPanel p;
    ThreadGroup g;
    DisplayMode fsmode = null, prefs = null;
    Dimension insetsSize;
    public static Dimension innerSize;
    public static Point centerPoint;
    public static Coord screenSZ;
    public static MainFrame instance;
	
    static {
	try {
	    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	} catch(Exception e) {}
    }
	
    DisplayMode findmode(int w, int h) {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(!dev.isFullScreenSupported())
	    return(null);
	DisplayMode b = null;
	for(DisplayMode m : dev.getDisplayModes()) {
	    int d = m.getBitDepth();
	    if((m.getWidth() == w) && (m.getHeight() == h) && ((d == 24) || (d == 32) || (d == DisplayMode.BIT_DEPTH_MULTI))) {
		if((b == null) || (d > b.getBitDepth()) || ((d == b.getBitDepth()) && (m.getRefreshRate() > b.getRefreshRate())))
		    b = m;
	    }
	}
	return(b);
    }
	
    public void setfs() {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(prefs != null)
	    return;
	prefs = dev.getDisplayMode();
	try {
	    setVisible(false);
	    dispose();
	    setUndecorated(true);
	    setVisible(true);
	    dev.setFullScreenWindow(this);
	    dev.setDisplayMode(fsmode);
			
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
    }
	
    public void setwnd() {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(prefs == null)
	    return;
	try {
	    dev.setDisplayMode(prefs);
	    dev.setFullScreenWindow(null);
	    setVisible(false);
	    dispose();
	    setUndecorated(false);
	    setVisible(true);
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
	prefs = null;
    }

    public boolean hasfs() {
	return(prefs != null);
    }

    public void togglefs() {
	if(prefs == null)
	    setfs();
	else
	    setwnd();
    }

    private void seticon() {
	Image icon;
	try {
	    InputStream data = MainFrame.class.getResourceAsStream("icon.png");
	    icon = javax.imageio.ImageIO.read(data);
	    data.close();
	} catch(IOException e) {
	    throw(new Error(e));
	}
	setIconImage(icon);
    }

    @Override
    public void setTitle(String charname) {
	String str = TITLE;
	if(charname != null){
	    str = charname+" - "+str;
	}
	super.setTitle(str);
    }

    public MainFrame(int w, int h) {
	super("");
	setTitle(null);
	instance = this;
	innerSize = new Dimension(w, h);
	centerPoint = new Point(innerSize.width / 2, innerSize.height / 2);
	screenSZ = new Coord(Toolkit.getDefaultToolkit().getScreenSize());
	p = new HavenPanel(w, h);
	fsmode = findmode(w, h);
	add(p);
	pack();
	Insets insets = getInsets();
	insetsSize = new Dimension(insets.left + insets.right, insets.top + insets.bottom);
	setResizable(true);
	setMinimumSize(new Dimension(800 + insetsSize.width, 600 + insetsSize.height));
	p.requestFocusInWindow();
	seticon();
	setVisible(true);
	p.init();
	
	new MainScript(p); // new
	
	if(Config.maxWindow){ // new
		setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
	}
	
    }

    public static Coord getScreenSize() {
        return screenSZ;
    }

    public static Coord getInnerSize() {
        return new Coord(innerSize.width, innerSize.height);
    }

    public static Coord getCenterPoint() {
        return new Coord(centerPoint.x, centerPoint.y);
    }
    
    public static void setupres() {
	if(ResCache.global != null)
	    Resource.addcache(ResCache.global);
	if(Config.resurl != null)
	    Resource.addurl(Config.resurl);
	if(ResCache.global != null) {
	    try {
		Resource.loadlist(ResCache.global.fetch("tmp/allused"), -10);
	    } catch(IOException e) {}
	}
	if(!Config.nopreload) {
	    try {
		InputStream pls;
		pls = Resource.class.getResourceAsStream("res-preload");
		if(pls != null)
		    Resource.loadlist(pls, -5);
		pls = Resource.class.getResourceAsStream("res-bgload");
		if(pls != null)
		    Resource.loadlist(pls, -10);
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	}
    }
    
    static {
	WebBrowser.self = JnlpBrowser.create();
    }
}
