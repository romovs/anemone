package haven;

import java.awt.Color;
import java.util.List;

public class SessionBar extends WindowTrans {
	public final static int ID = 1000;
	public static Coord initPos = new Coord(340, 10);
    private final static Tex bg = Resource.loadtex("gfx/hud/bgtex");
    private static final Tex missing = Resource.loadtex("gfx/hud/equip/missing");
    private static final Coord unborder = new Coord(2, 2);
    private static final Coord dasz = new Coord(74, 74);
    private Color color = Color.WHITE;
    private static final Coord avasz = new Coord(40,40);
    private static final int BORDER = 5;
    private static final int LEFT_OFFSET = 25;
    
    static {
		Widget.addtype("sessionbar", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
			    return(new SessionBar(c, parent));
			}
		    });
    }
    
    
    public SessionBar(Coord c, Widget parent) {
    	super(c, new Coord(LEFT_OFFSET+avasz.x+2*BORDER, 25), parent, null);
    	cbtn.visible = false;
    	fbtn.c = new Coord(0, 1);
    	fbtn.show();
    	mrgn = new Coord(0, 0);
    }
    

    public void draw(GOut g) {
		super.draw(g);
	
		if(folded)
		    return;
		
    	recalcsz(null);
		
		List<SessionData> sess = MaidFrame.getSessionList();
		for (int i = 0; i < sess.size(); i++)
			drawAvatar(g, i, sess.get(i));
    } 
    
    
    private void drawAvatar(GOut g, int index, SessionData sess) {
    	UI ui = sess.getUI();
    	if (ui == null)
    		return;

    	Tex at = sess.getAvatar();
    	
    	int avoffset = LEFT_OFFSET+index*(avasz.x+2*BORDER);
    	
    	// display empty box for login screen
    	if (at == null) {
    		g.chcolor(color);
    		Window.wbox.draw(g, Coord.z.add(avoffset, 0), avasz.add(Window.wbox.bisz()).add(unborder.mul(2).inv()));
    		g.image(bg, Coord.z.add(avoffset+BORDER, BORDER), avasz);
    		g.chcolor();	
    		return;
    	} 

		GOut g2 = g.reclip(Window.wbox.tloff().add(unborder.inv()).add(avoffset, 0), avasz);
		// g2.image(Equipory.bg, new Coord(Equipory.bg.sz().x / 2 - asz.x / 2, 20).inv().add(off));
		int yo = (20 * avasz.y) / dasz.y;
		Coord tsz = new Coord((at.sz().x * avasz.x) / dasz.x, (at.sz().y * avasz.y) / dasz.y);
		g2.image(bg, new Coord(tsz.x / 2 - avasz.x / 2, yo).inv(), tsz);
		g2.image(at, new Coord(tsz.x / 2 - avasz.x / 2, yo).inv(), tsz);

		if (MaidFrame.getCurrentSession() == sess) {
			g2.chcolor(Color.RED);
			g2.rect(new Coord(1, 1), avasz);
		}

		g.chcolor(color);
		
		Window.wbox.draw(g, Coord.z.add(avoffset, 0), avasz.add(Window.wbox.bisz()).add(unborder.mul(2).inv()));
		g.chcolor();	    
    }
    
    
    public void wdgmsg(Widget sender, String msg, Object... args) {
		if(sender == cbtn)
		    ui.destroy(this);
		if(sender == fbtn)
		    super.wdgmsg(sender, msg, args);
    }

    private int getClickedAvatarIndex(int x) {
    	int index = (x-LEFT_OFFSET)/(2*BORDER+avasz.x);
    	
    	if (x <= LEFT_OFFSET || index >= MaidFrame.getSessionList().size())
    		return -1;
    	
    	return index;
    }
    
    @Override
    protected void recalcsz(Coord max) {
    	if(folded) {
			wsz.x = 15;
			wsz.y = 15;
		}
		else {
			wsz.x = LEFT_OFFSET + (avasz.x+2*BORDER)*MaidFrame.getSessionCount();
			wsz.y = avasz.y+2*BORDER;
		}
    	sz = asz = wsz;
    }

    @Override
    public boolean mouseup(Coord c, int button) {
		if (button == 1) {
			int i = getClickedAvatarIndex(c.x);
			
			if (i >= 0)
				MaidFrame.switchToSession(i);

		    ui.grabmouse(null);
		}
		super.mouseup(c, button);
		
		return (true);
    }
}
