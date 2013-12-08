package haven;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

public class FoodMeterWidget extends Widget {
	public static final String NAME = "FoodMeterWdg";
	private static final Coord initPos = new Coord(10, 175);
    boolean dm = false;
    public Coord doff;
	int cap;
	List<El> els = new LinkedList<El>();
	
	private class El {
	    String id;
	    int amount;
	    Color col;
	    
	    public El(String id, int amount, Color col) {
		this.id = id;
		this.amount = amount;
		this.col = col;
	    }
	}
	
	public FoodMeterWidget(Coord c, Widget parent) {
	    super(c, CharWnd.foodmimg.sz(), parent);
		loadpos();
	}
    
	public void draw(GOut g) {
		if (Config.fepbar) {
		    g.chcolor(Color.BLACK);
		    g.frect(new Coord(4, 4), sz.add(-8, -8));
		    g.chcolor(255, 255, 255, 128);
		    g.image(CharWnd.foodmimg, Coord.z);
		    g.chcolor();
		    synchronized(els) {
			int x = 4;
			for(El el : els) {
			    int w = (174 * el.amount) / cap;
			    g.chcolor(el.col);
			    g.frect(new Coord(x, 4), new Coord(w, 24));
			    x += w;
			}
			g.chcolor();
		    }
		    g.chcolor(255, 255, 255, 128);
		    g.image(CharWnd.foodmimg, Coord.z);
		    g.chcolor();
		    super.draw(g);
		}
	}
	
	public void update(Object... args) {
	    cap = (Integer)args[0];
	    int sum = 0;
	    synchronized(els) {
		els.clear();
		for(int i = 1; i < args.length; i += 3) {
		    String id = (String)args[i];
		    int amount = (Integer)args[i + 1];
		    Color col = (Color)args[i + 2];
		    els.add(new El(id, amount, col));
		    sum += amount;
		}
	    }
	    if(els.size() == 0) {
		tooltip = String.format("0 of %.1f", cap / 10.0);
	    } else {
		String tt = "";
		for(El el : els)
		    tt += String.format("%.1f %s + ", el.amount / 10.0, el.id);
		tt = tt.substring(0, tt.length() - 3);
		tooltip = String.format("(%s) = %.1f of %.1f", tt, sum / 10.0, cap / 10.0);
	    }
	}
	
	@Override
	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);
		raise();
		if(super.mousedown(c, button))
		    return(true);
		if(button == 1) {
		    ui.grabmouse(this);
		    dm = true;
		    doff = c;
		}
		return(true);
	}
	
	@Override
    public boolean mouseup(Coord c, int button) {
	if(dm) {
	    ui.grabmouse(null);
	    dm = false;
	    storepos();
	} else {
	    super.mouseup(c, button);
	}
	return(true);
    }
	
	@Override
    public void mousemove(Coord c) {
	if(dm) {
	    this.c = this.c.add(c.add(doff.inv()));
	    List<SessionData> sesList = MaidFrame.getSessionList();
	    for (SessionData s : sesList) {
	    	if (s != null && s.getFoodMeter() != null)
	    		s.getFoodMeter().c = this.c;
	    }
	} else {
	    super.mousemove(c);
	}
    }
    
    private void storepos(){
	    Config.setWindowOpt(NAME + "_pos", c.toString());
    }
    
    private void loadpos() {
    	c = new Coord(Config.window_props.getProperty(NAME + "_pos", initPos.toString()));
    }
}