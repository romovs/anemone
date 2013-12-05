package haven;

import haven.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaidUI extends UI {
	private static final Pattern progress = Pattern.compile("gfx/hud/prog/(\\d+)");
	private static final Pattern cursorName = Pattern.compile("gfx/hud/curs/(.+)");
	
	private final Maid maid;
	
	private int progressWdgId;

	public MaidUI(Maid maid, Coord c, Session ses) {
		super(c, ses);
		this.maid = maid;
	}

	@Override
	public void setreceiver(final Receiver rcvr) {
		this.rcvr = new Receiver() {
			public void rcvmsg(int id, String msg, Object... args) {
				System.out.print("rcvmsg\tid: " + id + "\tmsg:" + msg);
				for(int i = 0; i < args.length; i++) {
					System.out.print(" " + args[i]);
				}
				System.out.println();
				
				rcvr.rcvmsg(id, msg, args);
			}
		};
	}

	@Override
	public void newwidget(int id, String type, Coord c, int parent, Object... args) throws InterruptedException {
		System.out.print("newwidget\tid: " + id + "\ttype:" + type + "\tcoord: " + c + "\tparent:" + parent + "\targs:");
		for(int i = 0; i < args.length; i++) {
			System.out.print(" " + args[i]);
		}
		System.out.println();
		
		
		WidgetFactory f;
		if(type.indexOf('/') >= 0) {
		    int ver = -1, p;
		    if((p = type.indexOf(':')) > 0) {
			ver = Integer.parseInt(type.substring(p + 1));
			type = type.substring(0, p);
		    }
		    Resource res = Resource.load(type, ver);
		    res.loadwaitint();
		    f = res.layer(Resource.CodeEntry.class).get(WidgetFactory.class);
		} else {
		    f = Widget.gettype(type);
		}
		synchronized(this) {
		    Widget pwdg = widgets.get(parent);
		    if(pwdg == null)
			throw(new UIException("Null parent widget " + parent + " for " + id, type, args));
		    
		    Widget wdg ;
		    if (type.equals("chr")) {
			    int studyid = -1;
			    if(args.length > 0)
				studyid = (Integer)args[0];
			    wdg = new CharWnd(c, pwdg, studyid);
			    MaidFrame.getCurrentSession().charWnd = (CharWnd)wdg;
		    } else if (type.equals("buddy")) { 
		    	wdg = new BuddyWnd(c, pwdg);
		    	MaidFrame.getCurrentSession().buddyWnd = (BuddyWnd)wdg;
		    } else if (type.equals("av")) {
		    	wdg = f.create(c, pwdg, args);
		    	MaidFrame.getCurrentSession().setAvatar((Avaview)wdg);
		    } else {
		    	wdg = f.create(c, pwdg, args);
		    }

		    bind(wdg, id);
		    wdg.binded();
		    if(wdg instanceof MapView) {
		    	mainview = (MapView)wdg;
		    	
		    	MaidFrame.getCurrentSession().sb = new SessionBar(SessionBar.initPos, pwdg);
		    	if (MaidFrame.getSessionCount() == 1)
		    		MaidFrame.getCurrentSession().sb.visible = false;
		    }
		}
		

		if ("scm".equals(type)) {
			maid.setMenuGridId(id);
		} else {
			try {
				if (maid.getTaskListener() != null && "img".equals(type)) {
					onImgChange(maid.getTaskListener(), id, (String) args[0]);
				} else if (maid.getItemListener() != null && "item".equals(type)) {
					onItemDisplay(maid.getItemListener(), (Item) widgets.get(id));
				} else if (maid.getWidgetListener() != null && "sm".equals(type)) {
					onWidgetCreate(maid.getWidgetListener(), (FlowerMenu) widgets.get(id));
				} else if (maid.getWidgetListener() != null && "inv".equals(type)) {
					onWidgetCreate(maid.getWidgetListener(), (Inventory) widgets.get(id));
				} else if (maid.getWidgetListener() != null && "make".equals(type)) {
					onWidgetCreate(maid.getWidgetListener(), (Makewindow) widgets.get(id));
				}
			} catch (Throwable t) {
				errorInEventProcessing(t);
			}
		}
	}

	@Override
	public void uimsg(int id, String msg, Object... args) {
		Widget wdg;
		synchronized(this) {
		    wdg = widgets.get(id);
		}
		if(wdg != null)
		    wdg.uimsg(msg.intern(), args);
		else
		    throw(new UIException("Uimsg to non-existent widget " + id, msg, args));
		
		System.out.print("uimsg\tid: " + id + "\tmsg:" + msg);
		for(int i = 0; i < args.length; i++) {
			System.out.print(" " + args[i]);
		}
		System.out.println();
		
		try {
		    wdg = widgets.get(id);
			if (maid.getMeterListener() != null && wdg instanceof IMeter && "set".equals(msg)) {
				onIMeterChange(maid.getMeterListener(), (IMeter) wdg, args);
			} else if (maid.getTaskListener() != null && wdg instanceof Img && "ch".equals(msg)) {
				onImgChange(maid.getTaskListener(), id, (String) args[0]);
			} else if (maid.getCursorListener() != null && "curs".equals(msg)) {
				onCursChange(maid.getCursorListener(), (String) args[0]);
			} else if (wdg instanceof IMeter && "set".equals(msg)) {	// update Maid.meter*	
				String name = ((IMeter)wdg).bg.name;

				if ("gfx/hud/meter/hp".equals(name)) {
					//TODO
				} else if ("gfx/hud/meter/nrj".equals(name)) {				
					maid.meterStamina = (MeterEventObjectStamina)new MeterEvent(MeterEvent.Type.STAMINA, args).getEventObject();
				} else if ("gfx/hud/meter/hngr".equals(name)) {
					maid.meterHunger = (MeterEventObjectHunger)new MeterEvent(MeterEvent.Type.HUNGER, args).getEventObject();
				} else if ("gfx/hud/meter/happy".equals(name)) {
					//TODO
				} else if ("gfx/hud/meter/auth".equals(name)) {
					//TODO
				}				
			} else if (maid.getPackListener() != null && "pack".equals(msg)) {
				maid.getPackListener().onPackExecute(new PackEvent());
			}
		} catch (Throwable t) {
			errorInEventProcessing(t);
		}
	}

	@Override
	public void destroy(int id) {
		Widget wdg = widgets.get(id);
		
		System.out.println("destroy\tid: " + id);
		
		try {
			if (maid.getTaskListener() != null && wdg instanceof Img) {
				onImgDestroy(maid.getTaskListener(), id);
			} else if (maid.getItemListener() != null && wdg instanceof Item) {
				onItemDestroy(maid.getItemListener(), (Item) wdg);
			}
		} catch (Throwable t) {
			errorInEventProcessing(t);
		}
		
		synchronized(this) {
		    if(widgets.containsKey(id)) {
			wdg = widgets.get(id);
			destroy(wdg);
		    }
		}
	}
	
	private void errorInEventProcessing(Throwable t) {
		t.printStackTrace();
		
		maid.doErr("Error processing events, canceling everything");
		maid.clearListeners();
		maid.stopTask();
	}
	
	private void onItemDisplay(ItemListener l, Item item) {
		if (item.parent instanceof RootWidget) {
			l.onItemGrab(new ItemEvent(ItemEvent.Type.GRAB, item));
		} else {
			l.onItemCreate(new ItemEvent(ItemEvent.Type.CREATE, item));
		}
	}
	
	private void onItemDestroy(ItemListener l, Item item) {
		if (item.parent instanceof RootWidget) {
			l.onItemRelease(new ItemEvent(ItemEvent.Type.RELEASE, item));
		} else {
			l.onItemDestroy(new ItemEvent(ItemEvent.Type.DESTROY, item));
		}
	}
	
	private void onIMeterChange(MeterListener l, IMeter im, Object[] args) {
		String name = im.bg.name;

		if ("gfx/hud/meter/hp".equals(name)) {
			l.onHealChange(new MeterEvent(MeterEvent.Type.HP, args));
		} else if ("gfx/hud/meter/nrj".equals(name)) {
			l.onStaminaChange(new MeterEvent(MeterEvent.Type.STAMINA, args));
		} else if ("gfx/hud/meter/hngr".equals(name)) {
			l.onHungerChange(new MeterEvent(MeterEvent.Type.HUNGER, args));
		} else if ("gfx/hud/meter/happy".equals(name)) {
			l.onHappinessChange(new MeterEvent(MeterEvent.Type.HAPINESS, args));
		} else if ("gfx/hud/meter/auth".equals(name)) {
			l.onAuthorityChange(new MeterEvent(MeterEvent.Type.AUTHORITY, args));
		}
	}
	
	private void onImgChange(TaskListener l, int id, String res) {
		Matcher m;
		if ((m = progress.matcher(res)).matches()) {
			int value = Math.round(Float.parseFloat(m.group(1)) / 20 * 100);

			progressWdgId = id;

			l.onTaskProgress(new TaskEvent(value));
		}
	}
	
	private void onImgDestroy(TaskListener l, int id) {
		if (id == progressWdgId) {
			TaskEvent e = new TaskEvent(100);
			l.onTaskProgress(e);
			l.onTaskComplete(e);
		}
	}
	
	private void onCursChange(CursorListener l, String res) {
		Matcher m;
		if ((m = cursorName.matcher(res)).matches()) {
			res = m.group(1);
		}
		l.onCursorChange(new CursorEvent(res));
	}
	
	private void onWidgetCreate(WidgetListener<?> l, Widget wdg) {
		System.out.println(wdg);
		
		Class<?> c = l.getInterest();
		if (c.isInstance(wdg)) {
			l.onCreate(new WidgetEvent(WidgetEvent.Type.CREATE, wdg));
		}
	}
}
