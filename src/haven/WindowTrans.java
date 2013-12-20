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

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WindowTrans extends Widget implements DTarget {
	private final static Set<String> storePosSet = new HashSet<String>();
	protected static BufferedImage[] cbtni = new BufferedImage[] { Resource.loadimg("gfx/hud/cbtn"), Resource.loadimg("gfx/hud/cbtnd"),
			Resource.loadimg("gfx/hud/cbtnh") };
	protected static BufferedImage[] fbtni = new BufferedImage[] { Resource.loadimg("gfx/hud/fbtn"), Resource.loadimg("gfx/hud/fbtnd"),
			Resource.loadimg("gfx/hud/fbtnh") };
	static Color cc = Color.YELLOW;
	static Text.Foundry cf = new Text.Foundry(new Font("Serif", Font.PLAIN, 12));
	boolean dt = false;
	public boolean justclose = false;
	public Text cap;
	boolean dm = false;
	public Coord atl, asz, wsz = new Coord();
	public Coord tlo, rbo;
	public Coord mrgn = new Coord(0, 0);
	public Coord doff;
	public IButton cbtn;
	public IButton fbtn;
	public boolean folded;
	ArrayList<Widget> wfolded;
	protected Coord ssz;

	static {
		Widget.addtype("wndtrans", new WidgetFactory() {
			public Widget create(Coord c, Widget parent, Object[] args) {
				if (args.length < 2)
					return (new WindowTrans(c, (Coord) args[0], parent, null));
				else
					return (new WindowTrans(c, (Coord) args[0], parent, (String) args[1]));
			}
		});
	}

	protected void placecbtn() {
		cbtn.c = new Coord(wsz.x - 3 - Utils.imgsz(cbtni[0]).x, 3).add(mrgn.inv());
		fbtn.c = new Coord(cbtn.c.x - 1 - Utils.imgsz(fbtni[0]).x, cbtn.c.y);
	}

	public WindowTrans(Coord c, Coord sz, Widget parent, String cap, Coord tlo, Coord rbo) {
		super(c, new Coord(0, 0), parent);
		this.tlo = tlo;
		this.rbo = rbo;
		cbtn = new IButton(Coord.z, this, cbtni[0], cbtni[1], cbtni[2]);
		fbtn = new IButton(Coord.z, this, fbtni[0], fbtni[1], fbtni[2]);
		fbtn.hide();
		folded = false;
		wfolded = new ArrayList<Widget>();
		if (cap != null)
			this.cap = cf.render(cap, cc);
		ssz = new Coord(sz);
		sz = sz.add(tlo).add(rbo).add(mrgn.mul(2));
		this.sz = sz;
		atl = tlo;
		wsz = sz.add(tlo.inv()).add(rbo.inv());
		asz = new Coord(wsz.x - mrgn.x, wsz.y - mrgn.y);
		placecbtn();
		setfocustab(true);
		parent.setfocus(this);
		loadpos();
	}

	public WindowTrans(Coord c, Coord sz, Widget parent, String cap) {
		this(c, sz, parent, cap, new Coord(0, 0), new Coord(0, 0));
	}

	public void cdraw(GOut g) {
	}

	public void draw(GOut og) {
		super.draw(og);
	}

	public void checkfold() {
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if ((wdg == cbtn) || (wdg == fbtn))
				continue;
			if (folded) {
				if (wdg.visible) {
					wdg.hide();
					wfolded.add(wdg);
				}
			} else if (wfolded.contains(wdg)) {
				wdg.show();
			}
		}
		Coord max = new Coord(ssz);
		if (folded) {
			max.y = 0;
		} else {
			wfolded.clear();
		}

		recalcsz(max);
	}

	protected void recalcsz(Coord max) {
		sz = max.add(mrgn.mul(2).add(tlo).add(rbo)).add(-1, -1);
		wsz = sz.sub(tlo).sub(rbo);
		if (folded)
			wsz.y = wsz.y / 2;
		asz = wsz.sub(mrgn.mul(2));
	}

	public void pack() {
		Coord max = new Coord(0, 0);
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if ((wdg == cbtn) || (wdg == fbtn))
				continue;
			Coord br = wdg.c.add(wdg.sz);
			if (br.x > max.x)
				max.x = br.x;
			if (br.y > max.y)
				max.y = br.y;
		}
		ssz = max;
		checkfold();
		placecbtn();
	}

	public void uimsg(String msg, Object... args) {
		if (msg == "pack") {
			pack();
		} else if (msg == "dt") {
			dt = (Integer) args[0] != 0;
		} else {
			super.uimsg(msg, args);
		}
	}

	public Coord xlate(Coord c, boolean in) {
		if (in)
			return (c.add(tlo).add(mrgn));
		else
			return (c.add(tlo.inv()).add(mrgn.inv()));
	}

	public boolean mousedown(Coord c, int button) {
		parent.setfocus(this);
		raise();
		if (super.mousedown(c, button))
			return (true);
		if (!c.isect(tlo, sz.add(tlo.inv()).add(rbo.inv())))
			return (false);
		if (button == 1) {
			ui.grabmouse(this);
			dm = true;
			doff = c;
		}
		return (true);
	}

	public boolean mouseup(Coord c, int button) {
		if (dm) {
			ui.grabmouse(null);
			dm = false;
			storepos();
		} else {
			super.mouseup(c, button);
		}
		return (true);
	}

	public void mousemove(Coord c) {
		if (dm) {
			this.c = this.c.add(c.add(doff.inv()));
		} else {
			super.mousemove(c);
		}
	}

	private void storepos() {
		if (cap == null) {
			return;
		}
		String name = cap.text;
		if (storePosSet.contains(name)) {
			Config.setWindowOpt(name + "_pos", c.toString());
			return;
		}
	}

	private void loadpos() {
		if (cap == null) {
			return;
		}
		String name = cap.text;
		if (storePosSet.contains(name)) {
			c = new Coord(Config.window_props.getProperty(name + "_pos", c.toString()));
			return;
		}
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == cbtn) {
			if (justclose)
				ui.destroy(this);
			else
				wdgmsg("close");
		} else if (sender == fbtn) {
			folded = !folded;
			checkfold();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	public boolean type(char key, java.awt.event.KeyEvent ev) {
		if (key == 27) {
			if (justclose)
				ui.destroy(this);
			else
				wdgmsg("close");
			return (true);
		}
		return (super.type(key, ev));
	}

	public boolean drop(Coord cc, Coord ul) {
		if (dt) {
			wdgmsg("drop", cc);
			return (true);
		}
		return (false);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
		return (false);
	}

	public Object tooltip(Coord c, boolean again) {
		Object ret = super.tooltip(c, again);
		if (ret != null)
			return (ret);
		else
			return ("");
	}
}
