package haven;

import groovy.lang.Binding;
import groovy.transform.ThreadInterrupt;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import haven.event.*;
import haven.pathfinder.AStar;
import haven.pathfinder.DbgWnd;
import haven.pathfinder.Map;
import haven.pathfinder.Node;
import haven.pathfinder.PathFinder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class Maid {

	private final Object LOCK = new Object();
	private final String scripts_folder;
	private final String scripts[];
	private final GroovyScriptEngine engine;
	private final Binding binding;
	private final ThreadGroup taskGroup;
	private Thread task, wait;
	private TaskListener taskListener;
	private CursorListener cursorListener;
	private MeterListener meterListener;
	private ItemListener itemListener;
	private WidgetListener<?> widgetListener;
	private PackListener packListener;
	private int menuGridId = 0;
	private HWindow areaChat;
	public MaidUI ui;
	private final static float ITEM_WIDTH = 31;
	private final static float ITEM_HEIGHT = 32;
	
	// helper objects for getting current meter values for situations when event
	// based mechanism is not needed
	public MeterEventObjectHunger meterHunger;
	public MeterEventObjectStamina meterStamina;

	public Maid() {
		taskGroup = new ThreadGroup("groovy");

		binding = new Binding();
		binding.setVariable("maid", this);

		Properties p = initConfig();

		scripts = initScripts(p);
		scripts_folder = initScriptFolder(p);

		engine = initGroovy(scripts_folder);
		engine.getConfig().addCompilationCustomizers(new org.codehaus.groovy.control.customizers.ASTTransformationCustomizer(ThreadInterrupt.class));
	}

	private Properties initConfig() {
		Properties p = new Properties();

		File inputFile = new File("maid.conf");
		if (!inputFile.exists()) {
			return p;
		}

		try {
			p.load(new FileInputStream(inputFile));
		} catch (IOException e) {
		}

		return p;
	}

	private String[] initScripts(Properties p) {
		String[] s = new String[12];
		for (int i = 1; i <= 12; i++) {
			s[i - 1] = p.getProperty("script_f" + i, "f" + i);
		}
		return s;
	}

	private String initScriptFolder(Properties p) {
		return p.getProperty("scripts_folder", "scripts");
	}

	private GroovyScriptEngine initGroovy(String scripts_folder) {
		GroovyScriptEngine gse;
		try {
			gse = new GroovyScriptEngine(scripts_folder);
		} catch (IOException e) {
			doErr("Can't open scripts folder. I will try creating it...");

			boolean success = new File(scripts_folder).mkdir();

			if (success) {
				try {
					gse = new GroovyScriptEngine(scripts_folder);
				} catch (IOException e2) {
					doErr("Directory \"" + scripts_folder + "\" gives errors. I give up.");

					throw new RuntimeException("Can't initialize groovy script engine.", e2);
				}
			} else {
				doErr("Can't read/create \"" + scripts_folder + "\".");

				throw new RuntimeException("Can't initialize groovy script engine.", e);
			}
		}

		return gse;
	}

	public void doSay(Object text) {
		System.out.println(text);
	}

	public void doErr(Object text) {
		System.err.println(text);
	}

	void doTask(final String name, final String... args) {
		if (task != null) {
			doSay("-- Already running a task.");
			return;
		}
		task = new Thread(taskGroup, "maid") {

			@Override
			public void run() {
				binding.setVariable("args", args);
				try {
					preProcessing();
					engine.run(name + ".groovy", binding);

					postProcessing();

					doSay("-- Done\n");
				} catch (ResourceException e) {
					doSay("Can't find the file.");

					e.printStackTrace();
				} catch (ScriptException e) {
					doSay("Something is wrong with this task. I don't understand it.");

					e.printStackTrace();
				} catch (Throwable t) {
					doErr("Canceled?");

					t.printStackTrace();
				} finally {
					task = null;
				}
			}
		};

		task.start();
	}

	void doTask(int i) {
		doTask(scripts[i]);
	}

	void stopTask() {
		if (task != null) {
			if (wait == null) {
				doSay("Interruping...");
				wait = new Thread() {

					@Override
					public void run() {
						doSay(task.toString());

						task.getThreadGroup().interrupt();

						wait = null;

						postProcessing();

						doSay("Interrupted successfuly.");
					}
				};

				wait.start();
			} else {
				doSay("Already interrumpting.");
			}

		} else {
			doSay("Nothing to interrupt.");
		}
	}

	private void preProcessing() {
	}

	private void postProcessing() {
		clearListeners();
	}

	/*
	 * void setHaven(HavenPanel haven) { this.haven = haven; }
	 */

	void setMenuGridId(int menuGridId) {
		this.menuGridId = menuGridId;
	}

	public CursorListener getCursorListener() {
		return cursorListener;
	}

	public void setCursorListener(CursorListener cursorListener) {
		this.cursorListener = cursorListener;
	}

	public ItemListener getItemListener() {
		return itemListener;
	}

	public void setItemListener(ItemListener itemListener) {
		this.itemListener = itemListener;
	}

	public MeterListener getMeterListener() {
		return meterListener;
	}

	public void setMeterListener(MeterListener meterListener) {
		this.meterListener = meterListener;
	}

	public TaskListener getTaskListener() {
		return taskListener;
	}

	public void setTaskListener(TaskListener taskListener) {
		this.taskListener = taskListener;
	}

	public WidgetListener<?> getWidgetListener() {
		return widgetListener;
	}

	public void setWidgetListener(WidgetListener<?> widgetListener) {
		this.widgetListener = widgetListener;
	}

	public PackListener getPackListener() {
		return packListener;
	}

	public void setPackListener(PackListener packListener) {
		this.packListener = packListener;
	}

	void clearListeners() {
		cursorListener = null;
		itemListener = null;
		meterListener = null;
		taskListener = null;
		widgetListener = null;
		packListener = null;
	}

	public void sleep() throws InterruptedException {
		synchronized (LOCK) {
			LOCK.wait();
		}
	}

	public void wakeup() {
		synchronized (LOCK) {
			LOCK.notify();
		}
	}

	public String waitForCursor() throws InterruptedException {
		final String[] retval = new String[1];

		cursorListener = new CursorListener() {

			public void onCursorChange(CursorEvent e) {
				retval[0] = e.getName();
				wakeup();
			}
		};

		sleep();

		cursorListener = null;

		return retval[0];
	}

	public Item waitForGrab() throws InterruptedException {
		final Item[] retval = new Item[1];
		itemListener = new ItemAdapter() {

			@Override
			public void onItemGrab(ItemEvent e) {
				retval[0] = e.getItem();

				wakeup();
			}
		};

		sleep();

		itemListener = null;

		return retval[0];
	}

	public Item waitForRelease() throws InterruptedException {
		final Item[] retval = new Item[1];
		itemListener = new ItemAdapter() {

			@Override
			public void onItemRelease(ItemEvent e) {
				retval[0] = e.getItem();

				wakeup();
			}
		};

		sleep();

		itemListener = null;

		return retval[0];
	}

	public Item waitForItemCreate() throws InterruptedException {
		final Item[] retval = new Item[1];
		itemListener = new ItemAdapter() {

			@Override
			public void onItemCreate(ItemEvent e) {
				retval[0] = e.getItem();

				wakeup();
			}
		};

		sleep();

		itemListener = null;

		return retval[0];
	}

	/**
	 * Blocks until the specified number of items have been created.
	 * 
	 * @param itemCount
	 *            number of items to wait for
	 * @return array containing created items
	 */
	public Item[] waitForItemCreate(final int itemCount) throws InterruptedException {
		final Item[] retval = new Item[itemCount];
		final int[] counter = new int[1];
		counter[0] = 0;

		itemListener = new ItemAdapter() {
			@Override
			public void onItemCreate(ItemEvent e) {
				retval[counter[0]++] = e.getItem();
				if (counter[0] == itemCount)
					wakeup();
			}
		};

		sleep();

		itemListener = null;

		return retval;
	}

	public Item waitForItemDestroy() throws InterruptedException {
		final Item[] retval = new Item[1];
		itemListener = new ItemAdapter() {

			@Override
			public void onItemDestroy(ItemEvent e) {
				retval[0] = e.getItem();

				wakeup();
			}
		};

		sleep();

		itemListener = null;

		return retval[0];
	}

	public void waitForTask() throws InterruptedException {
		taskListener = new TaskAdapter() {

			@Override
			public void onTaskComplete(TaskEvent taskEvent) {
				wakeup();
			}
		};

		sleep();

		taskListener = null;
	}

	public FlowerMenu waitForFlowerMenu() throws InterruptedException {
		final FlowerMenu retval[] = new FlowerMenu[1];

		widgetListener = new WidgetListener<FlowerMenu>() {

			public Class<FlowerMenu> getInterest() {
				return FlowerMenu.class;
			}

			public void onCreate(WidgetEvent<FlowerMenu> e) {
				retval[0] = e.getWidget();

				wakeup();
			}

			public void onDestroy(WidgetEvent<FlowerMenu> e) {
			}
		};

		sleep();

		widgetListener = null;

		return retval[0];
	}
	
	public FlowerMenu waitForFlowerMenu(int timeout) throws InterruptedException {
		final FlowerMenu retval[] = new FlowerMenu[1];

		widgetListener = new WidgetListener<FlowerMenu>() {

			public Class<FlowerMenu> getInterest() {
				return FlowerMenu.class;
			}

			public void onCreate(WidgetEvent<FlowerMenu> e) {
				retval[0] = e.getWidget();

				wakeup();
			}

			public void onDestroy(WidgetEvent<FlowerMenu> e) {
			}
		};
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
				  wakeup();
			  }
			}, timeout);

		sleep();

		widgetListener = null;

		return retval[0];
	}

	public Makewindow waitForMakeWindow(String name) throws InterruptedException {
		final Makewindow retval[] = new Makewindow[1];

		widgetListener = new WidgetListener<Makewindow>() {

			public Class<Makewindow> getInterest() {
				return Makewindow.class;
			}

			public void onCreate(WidgetEvent<Makewindow> e) {
				retval[0] = (Makewindow) e.getWidget();
				wakeup();
			}

			public void onDestroy(WidgetEvent<Makewindow> e) {
			}
		};

		sleep();

		widgetListener = null;

		return retval[0];
	}

	// Wait for player to stop moving
	public void waitForMoveStop() throws InterruptedException {
		final Gob gob = getPlayer();
		gob.movementListener = new MovementAdapter() {
			@Override
			public void onMovementStop(MovementEvent taskEvent) {
				wakeup();
			}
		};
		
		sleep();

		gob.movementListener = null;
	}
	
	
	// Wait for player to stop moving
	public void waitForMoveStop(int delay, int interval) throws InterruptedException {
		final Gob gob = getPlayer();
		
		// since event sometime is set out of order
		// check position at the specified intervals
		// and wakeup if too long in same position
		final Coord prevPos[] = new Coord[1];
		final Timer timer = new Timer();
	    prevPos[0] = gob.getc();
		timer.scheduleAtFixedRate(new TimerTask() {
			  @Override
			  public void run() {
				  if (prevPos[0].equals(getCoord())) {
					  wakeup();
					  timer.cancel();
				  }
				  else
					  prevPos[0] = gob.getc();
			  }
			}, delay, interval);

		gob.movementListener = new MovementAdapter() {
			@Override
			public void onMovementStop(MovementEvent taskEvent) {
				timer.cancel();
				wakeup();
			}
		};
		
		sleep();

		gob.movementListener = null;
	}

	// Wait for inventory (as in Widget of type Inventory, not user "inventory")
	// and all its items to be created.
	// Basically we just wait for Inventory Widget to be created and then wait
	// for 'pack' message telling us that
	// all items have been loaded.
	public void waitForInventory() throws InterruptedException {

		widgetListener = new WidgetListener<Inventory>() {

			public Class<Inventory> getInterest() {
				return Inventory.class;
			}

			@Override
			public void onCreate(WidgetEvent<Inventory> e) {
				packListener = new PackListener() {
					@Override
					public void onPackExecute(PackEvent e) {
						wakeup();
					}
				};
			}

			@Override
			public void onDestroy(WidgetEvent<Inventory> e) {
			}
		};

		sleep();

		widgetListener = null;
		packListener = null;
	}
	
	public Window waitForWindow(final String title) throws InterruptedException {
		final Window[] retval = new Window[1];
		widgetListener = new WidgetListener<Window>() {

			public Class<Window> getInterest() {
				return Window.class;
			}

			@Override
			public void onCreate(final WidgetEvent<Window> ew) {
				packListener = new PackListener() {
					@Override
					public void onPackExecute(PackEvent ep) {
						Window win = ew.getWidget();
						if (win.cap.text.equals(title)) {
							retval[0] = win;
							wakeup();
						}
					}
				};
			}

			@Override
			public void onDestroy(WidgetEvent<Window> e) {
			}
		};

		sleep();

		widgetListener = null;
		packListener = null;
		
		return retval[0];
	}

	public void doLogout() {
		ui.close();
	}

	public Gob getPlayer() {
		return ui.sess.glob.oc.getgob(ui.mainview.playergob);
	}

	public Coord getScreenCenter() {
		Coord sc = new Coord((int) Math.round(ui.mainview.sz.x / 2), (int) Math.round(ui.mainview.sz.y / 2));
		return sc;
	}

	public String getCursorName() {
		return ui.root.getcurs(MainFrame.p.mousepos).basename();
	}

	public Coord getCoord(Gob gob) {
		if (gob != null) {
			return gob.getc();
		} else {
			return null;
		}
	}

	public Coord getCoord() {
		return getCoord(getPlayer());
	}

	public Resource getResource(Gob g) {
		Resource res = null;

		ResDrawable rd;
		Layered l;

		if ((rd = g.getattr(ResDrawable.class)) != null) {
			res = rd.res.get();
		} else if ((l = g.getattr(Layered.class)) != null) {
			res = l.base.get();
		}

		return res;
	}

	public String getName(Gob g) {
		String name = null;

		Resource res = getResource(g);

		if (res != null) {
			name = res.name;
		} else if (g.id > 0) {
			doErr("Resource missing for gob " + g.id);
		}

		return name;
	}

	public <C> C getWidget(Class<C> klass) {
		for (Widget w : ui.rwidgets.keySet()) {

			if (klass.isInstance(w)) {
				return klass.cast(w);
			}
		}
		return null;
	}

	public <C> C[] getWidgets(Class<C> klass) {
		List<C> widgets = new ArrayList<C>();
		for (Widget w : ui.rwidgets.keySet()) {

			if (klass.isInstance(w)) {
				widgets.add(klass.cast(w));
			}
		}
		return widgets.toArray((C[]) Array.newInstance(klass, widgets.size()));
	}

	public Inventory getInventory(String name) {
		for (Widget wdg = ui.root.child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof Window) {
				Window window = (Window) wdg;
				if (window.cap != null && window.cap.text.equalsIgnoreCase(name)) {
					for (Widget w = wdg.child; w != null; w = w.next) {
						if (w instanceof Inventory) {
							return (Inventory) w;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns total number of slots in an inventory.
	 * 
	 * @param inv
	 *            Inventory object
	 * @return total number of slots
	 */
	public int getInventoryCapacity(Inventory inv) {
		return Math.round((float) inv.sz.x / ITEM_WIDTH) * Math.round((float) inv.sz.y / ITEM_HEIGHT);
	}

	/**
	 * Returns number of free slots in an inventory.
	 * 
	 * @param inv
	 *            Inventory object
	 * @return number of free slots
	 */
	public int getInventoryFreeSlots(Inventory inv) {
		int cap = getInventoryCapacity(inv);
		for (Widget i = inv.child; i != null; i = i.next) {
			if (i instanceof Item) {
				int itemSlots = Math.round((float) i.sz.x / ITEM_WIDTH) * Math.round((float) i.sz.y / ITEM_HEIGHT);
				cap -= itemSlots;
			}
		}
		return cap;
	}

	/**
	 * Returns user Inventory. Opens it if necessary.
	 * 
	 * @return Inventory object
	 */
	public Inventory getInventory() throws InterruptedException {
		Inventory inv = getInventory("Inventory");

		if (inv == null) {
			doOpenInventory();
			inv = getInventory("Inventory");
		}

		return inv;
	}

	public Item[] getItems(Inventory inv) {
		List<Item> items = new ArrayList<Item>(24);
		for (Widget i = inv.child; i != null; i = i.next) {
			if (i instanceof Item) {
				items.add((Item) i);
			}
		}
		return items.toArray(new Item[items.size()]);
	}

	// return only items matching itemsName
	public Item[] getItems(Inventory inv, String itemsName) {
		List<Item> items = new ArrayList<Item>(24);
		for (Widget i = inv.child; i != null; i = i.next) {
			if (i instanceof Item && getName((Item) i).toLowerCase().indexOf(itemsName) != -1) {
				items.add((Item) i);
			}
		}

		return items.toArray(new Item[items.size()]);
	}
	
	public Widget[] getImgWidgets(Window win) {
		List<Img> items = new ArrayList<Img>(24);
		for (Widget i = win.child; i != null; i = i.next) {
			if (i instanceof Img) {
				items.add((Img) i);
			}
		}

		return items.toArray(new Img[items.size()]);
	}
	
	public void doLight(Window win) {
		for (Widget i = win.child; i != null; i = i.next) {
			if (i instanceof Button && ((Button)i).text.text.equals("Light")) {
				((Button)i).click();
				break;
			}
		}
	}

	public String getName(Item i) {
		if (i.tooltip != null) {
			return (i.tooltip);
		}

		Resource res = i.res.get();
		if ((res != null) && (res.layer(Resource.tooltip) != null)) {
			return res.layer(Resource.tooltip).t;
		}

		return null;
	}

	public int getQuality(Item i) {
		return i.q;
	}

	public String getTooltip(Item i) {
		return i.tooltip;
	}

	public Coord getCoord(Item i) {
		return i.c.div(31);
	}

	public int getNumber(Item i) {
		return i.num;
	}

	public int getMeter(Item i) {
		return i.meter;
	}

	public Buff[] getBuffs() {
		Collection<Buff> c = ui.sess.glob.buffs.values();
		return c.toArray(new Buff[c.size()]);
	}

	public int getMeter(Buff b) {
		return b.ameter;
	}

	public int getTimeLeft(Buff b) {
		if (b.cmeter >= 0) {
			long now = System.currentTimeMillis();
			double m = b.cmeter / 100.0;
			if (b.cticks >= 0) {
				double ot = b.cticks * 0.06;
				double pt = ((double) (now - b.gettime)) / 1000.0;
				m *= (ot - pt) / ot;
			}
			return (int) Math.round(m * 100);
		}
		return 0;
	}

	public String getName(Buff b) {
		Resource r = b.res.get();

		if (r == null) {
			return "";
		}

		Resource.Tooltip tt = r.layer(Resource.tooltip);

		if (tt == null) {
			return "";
		}

		return tt.t;
	}

	public boolean isDragging() {
		for (Widget wdg = ui.root.child; wdg != null; wdg = wdg.next) {
			if ((wdg instanceof Item) && (((Item) wdg).dm)) {
				return true;
			}
		}
		return false;
	}

	public void doAction(String msg, Object... args) {
		if (menuGridId != 0) {
			ui.rcvr.rcvmsg(menuGridId, msg, args);
		} else {
			doErr("menuGrid not identified");
		}
	}

	public void doInteract(Coord mc, int modflags) {
		ui.mainview.wdgmsg("itemact", getScreenCenter(), mc, modflags);
	}

	public void doInteract(Coord mc) {
		doInteract(mc, 0);
	}

	public void doClick(Coord mc, int button, int modflags) {
		ui.mainview.wdgmsg("click", getScreenCenter(), mc, button, modflags);
	}

	public void doClick(Coord mc, int button) {
		doClick(mc, button, 0);
	}

	public void doLeftClick(Coord mc, int modflags) {
		doClick(mc, 1, modflags);
	}

	public void doLeftClick(Coord mc) {
		doClick(mc, 1, 0);
	}

	public void doRightClick(Coord mc, int modflags) {
		doClick(mc, 3, modflags);
	}

	public void doRightClick(Coord mc) {
		doClick(mc, 3, 0);
	}

	public void doClick(Gob gob, int button, int modflags) {
		Coord sc = getScreenCenter();
		Coord oc = gob.getc();
		ui.mainview.wdgmsg("click", sc, oc, button, modflags, gob.id, oc);
	}

	public void doClick(Gob gob, int button) {
		doClick(gob, button, 0);
	}

	public void doLeftClick(Gob gob, int modflags) {
		doClick(gob, 1, modflags);
	}

	public void doLeftClick(Gob gob) {
		doClick(gob, 1, 0);
	}

	public void doRightClick(Gob gob, int modflags) {
		doClick(gob, 3, modflags);
	}

	public void doRightClick(Gob gob) {
		doClick(gob, 3, 0);
	}

	public void doInteract(Gob gob, int modflags) {
		Coord sc = getScreenCenter();
		Coord oc = gob.getc();
		ui.mainview.wdgmsg("itemact", sc, oc, modflags, gob.id, oc);
	}

	public void doInteract(Gob gob) {
		doInteract(gob, 0);
	}

	public void doOpenInventory() throws InterruptedException {
		ui.root.wdgmsg("gk", 9);
		waitForInventory();
	}

	public void doTake(Item i) {
		i.wdgmsg("take", getScreenCenter());
	}

	public void doTransfer(Item i) {
		i.wdgmsg("transfer", getScreenCenter());
	}

	public void doDrop(Item i) {
		i.wdgmsg("drop", getScreenCenter());
	}

	public void doDrop(Item i, Coord c) {
		i.wdgmsg("drop", c);
	}

	public void doDrop(Item i, Widget wdg, Coord c) {
		wdg.wdgmsg("drop", c);
	}

	public void doInteract(Item i) {
		i.wdgmsg("iact", getScreenCenter());
	}

	public void doInteract(Item i, int mod) {
		i.wdgmsg("itemact", mod);
	}

	public Gob doAreaFind(Coord coord, double radius, String name) {
		double max = toTile(radius);

		Gob retval = null;

		synchronized (ui.sess.glob.oc) {
			for (Gob gob : ui.sess.glob.oc) {
				String gobName = getName(gob);

				if (gobName != null && gobName.indexOf(name) > 0) {
					double dist = gob.getc().dist(coord);
					if (dist < max) {
						max = dist;
						retval = gob;
					}
				}
			}
		}

		return retval;
	}

	public Gob doAreaFind(int offsetx, int offsety, double radius, String name) {
		Coord coord = getCoord().add(toTile(offsetx), toTile(offsety));

		return doAreaFind(coord, radius, name);
	}

	public Gob doAreaFind(double radius, String name) {
		return doAreaFind(0, 0, radius, name);
	}

	public Gob doAreaFindCrops(double radius, String name, int stage) {
		Coord coord = getCoord();
		double max = toTile(radius);

		Gob retval = null;

		synchronized (ui.sess.glob.oc) {
			for (Gob gob : ui.sess.glob.oc) {
				String gobName = getName(gob);

				if (gobName != null && gobName.indexOf(name) > 0) {
					double dist = gob.getc().dist(coord);
					if (dist < max && gob.getblob(0) == stage) {
						max = dist;
						retval = gob;
					}
				}
			}
		}

		return retval;
	}

	public Gob[] doAreaList(Coord coord, double radius) {
		List<Gob> list = new LinkedList<Gob>();

		double max = toTile(radius);

		synchronized (ui.sess.glob.oc) {
			for (Gob gob : ui.sess.glob.oc) {
				double dist = gob.getc().dist(coord);
				if (dist < max) {
					list.add(gob);
				}
			}
		}

		return list.toArray(new Gob[list.size()]);
	}

	public Gob[] doAreaList(int offsetx, int offsety, double radius) {
		Coord coord = getCoord().add(toTile(offsetx), toTile(offsety));

		return doAreaList(coord, radius);
	}

	public Gob[] doAreaList(double radius) {
		return doAreaList(0, 0, radius);
	}

	public String[] doList(FlowerMenu menu) {
		String names[] = new String[menu.opts.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = menu.opts[i].name;
		}
		return names;
	}

	public boolean doSelect(FlowerMenu menu, String option) {
		for (int i = 0; i < menu.opts.length; i++) {
			if (option.equalsIgnoreCase(menu.opts[i].name)) {
				menu.wdgmsg(menu, "cl", menu.opts[i].num);
				return true;
			}
		}
		return false;
	}

	public static int makeFlags(boolean shift, boolean ctrl, boolean alt, boolean meta) {
		int flags = 0;
		if (shift) {
			flags |= 1;
		}
		if (ctrl) {
			flags |= 2;
		}
		if (alt) {
			flags |= 4;
		}
		if (meta) {
			flags |= 8;
		}
		return flags;
	}

	public static int toTile(int i) {
		return i * MCache.tilesz.x;
	}

	public static double toTile(double i) {
		return i * MCache.tilesz.x;
	}

	public static Coord toTile(Coord coord) {
		Coord c = new Coord(coord);
		c = c.div(MCache.tilesz);
		c = c.mul(MCache.tilesz);
		c = c.add(MCache.tilesz.div(2));
		return (c);
	}

	public void doSayAreaChat(String str) {
		if (areaChat == null) {
			SlenHud panel = ui.slen;
			for (HWindow wnd : panel.wnds) {
				if (wnd.title.contains("Area Chat")) {
					areaChat = wnd;
					break;
				}
			}
		}
		areaChat.wdgmsg("msg", str);
	}

	public void doCraft(Makewindow mw) {
		mw.wdgmsg(mw.obtn, "activate");
	}

	public void doCraftAll(Makewindow mw) {
		mw.wdgmsg(mw.cbtn, "activate");
	}

	public Map getScene(int playerSize) {
		return new Map(1000, 1000, playerSize);
	}
	
	
	private DbgWnd dbgWin = null;

	
	private Coord toSceneCoord(Coord c) {
		MapView mv = getWidget(MapView.class);
		Coord frameSz = MainFrame.getInnerSize();
		Coord oc = MapView.viewoffsetFloorProjection(frameSz, mv.mc);
		return c.add(oc).sub(Map.MAP_COFFSET_X, 0);
	}
	
	private Coord fromSceneCoord(Coord c) {
		MapView mv = getWidget(MapView.class);
		Coord frameSz = MainFrame.getInnerSize();
		Coord oc = MapView.viewoffsetFloorProjection(frameSz, mv.mc);
		return c.sub(oc).add(Map.MAP_COFFSET_X, 0);
	}
	
	public boolean pathFindBoat(Coord dst, boolean dbg) {
		Gob player = getPlayer();
		if (dst.equals(player.getc()))
			return false;

		Map scene = getScene(26);

		MapView mv = getWidget(MapView.class);

		long start = DbgUtils.getCpuTime();
		scene.initSceneBoat(mv, player, dst, doAreaList(50.0d));
		long end = DbgUtils.getCpuTime();
		System.out.format("Scene Init Time: %s sec.\n", (double) (end - start) / 1000000000.0d);

		PathFinder finder = new AStar();

		if (dbg) {
			if (dbgWin == null) {
				dbgWin = new DbgWnd(scene, 1000, 1000);
				dbgWin.setVisible(true);
			} else {
				dbgWin.setScene(scene);
			}
		}

		Coord dstAdjusted = toSceneCoord(dst);
		
		System.out.println("SRC: " + toSceneCoord(player.getc()));
		System.out.println("DST: " + dstAdjusted);
		
		if (dstAdjusted.x > scene.w || dstAdjusted.y > scene.h ||
				dstAdjusted.x < 1 || dstAdjusted.y < 1) {
			System.err.println("!!!Destination is out of bounds!!!");
			return false;
		}
		
		start = DbgUtils.getCpuTime();
		List<Node> path = finder.find(scene, dstAdjusted, true);
		end = DbgUtils.getCpuTime();
		System.out.format("Finder Time: %s sec.\n", (double) (end - start) / 1000000000.0d);
		
		if (dbg)
			dbgWin.repaint();
		
		if (path == null) {
			System.out.println("!!!No path!!!");
			return false;
		}

		// should be calculated before starting moving
		List<Coord> realCoord = new ArrayList<Coord>();
		for (int i = 1; i < path.size(); i++) {
			Node n = path.get(i);
			realCoord.add(fromSceneCoord(new Coord(n.x, n.y)));
		}

		for (int i = 0; i < realCoord.size(); i++) {
			System.out.println("- pfboat click coord: " + realCoord.get(i));
			doLeftClick(realCoord.get(i));
			try {
				waitForMoveStop();
				System.out.println("- pf stopped at: " + getCoord());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	public boolean pathFind(Coord dst, int playerSize, boolean dbg) {
		Gob player = getPlayer();
		if (dst.equals(player.getc()))
			return false;
		
		Map scene = getScene(playerSize);

		MapView mv = getWidget(MapView.class);

		long start = DbgUtils.getCpuTime();
		scene.initScene(mv, player, dst, doAreaList(50.0d));
		long end = DbgUtils.getCpuTime();
		System.out.format("Scene Init Time: %s sec.\n", (double) (end - start) / 1000000000.0d);

		PathFinder finder = new AStar();

		if (dbg) {
			if (dbgWin == null) {
				dbgWin = new DbgWnd(scene, 1000, 1000);
				dbgWin.setVisible(true);
			} else {
				dbgWin.setScene(scene);
			}
		}
		
		Coord dstAdjusted = toSceneCoord(dst);
		
		System.out.println("SRC: " + toSceneCoord(player.getc()));
		System.out.println("DST: " + dstAdjusted);
		
		if (dstAdjusted.x > scene.w || dstAdjusted.y > scene.h ||
				dstAdjusted.x < 1 || dstAdjusted.y < 1) {
			System.err.println("!!!Destination is out of bounds!!!");
			return false;
		}

		start = DbgUtils.getCpuTime();
		List<Node> path = finder.find(scene, dstAdjusted, true);
		end = DbgUtils.getCpuTime();
		System.out.format("Finder Time: %s sec.\n", (double) (end - start) / 1000000000.0d);
		
		if (dbg)
			dbgWin.repaint();
		
		if (path == null) {
			System.out.println("!!!No path!!!");
			return false;
		}
		
		
		// should be calculated before starting moving
		List<Coord> realCoord = new ArrayList<Coord>();
		for (int i = 1; i < path.size(); i++) {
			Node n = path.get(i);
			realCoord.add(fromSceneCoord(new Coord(n.x, n.y)));
		}

		for (int i = 0; i < realCoord.size(); i++) {
			System.out.println("- pf click coord: " + realCoord.get(i));
			doLeftClick(realCoord.get(i));
			try {
				waitForMoveStop();
				System.out.println("- pf stopped");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean pathFindRightClick(Coord dst, int playerSize, boolean dbg) {
		Gob player = getPlayer();
		if (dst.equals(player.getc()))
			return false;
		
		Map scene = getScene(playerSize);

		MapView mv = getWidget(MapView.class);

		long start = DbgUtils.getCpuTime();
		scene.initScene(mv, player, dst, doAreaList(50.0d));
		long end = DbgUtils.getCpuTime();
		System.out.format("Scene Init Time: %s sec.\n", (double) (end - start) / 1000000000.0d);

		PathFinder finder = new AStar();

		if (dbg) {
			if (dbgWin == null) {
				dbgWin = new DbgWnd(scene, 1000, 1000);
				dbgWin.setVisible(true);
			} else {
				dbgWin.setScene(scene);
			}
		}

		Coord dstAdjusted = toSceneCoord(dst);
		
		System.out.println("SRC: " +  toSceneCoord(player.getc()));
		System.out.println("DST: " + dstAdjusted);
		
		if (dstAdjusted.x > scene.w || dstAdjusted.y > scene.h ||
				dstAdjusted.x < 1 || dstAdjusted.y < 1)
		{
			System.err.println("!!!Destination is out of bounds!!!");
			return false;
		}
		
		start = DbgUtils.getCpuTime();
		List<Node> path = finder.find(scene, toSceneCoord(dst), true);
		end = DbgUtils.getCpuTime();
		System.out.format("Finder Time: %s sec.\n", (double) (end - start) / 1000000000.0d);

		if (dbg)
			dbgWin.repaint();
		
		if (path == null) {
			System.err.println("!!!No path!!!");
			return false;
		}
		
		// should be calculated before starting moving
		List<Coord> realCoord = new ArrayList<Coord>();
		for (int i = 1; i < path.size(); i++) {
			Node n = path.get(i);
			realCoord.add(fromSceneCoord(new Coord(n.x, n.y)));
		}

		for (int i = 0; i < realCoord.size(); i++) {
			if (i == realCoord.size()-1) {
				doRightClick(realCoord.get(i));
				System.out.println("- pf right click coord: " + realCoord.get(i));
			} else {
				doLeftClick(realCoord.get(i));
				System.out.println("- pf click coord: " + realCoord.get(i));
			}
			
			try {
				System.out.println("- pf waiting for stop");
				waitForMoveStop(1000, 1000);
				System.out.println("- pf stopped");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	public Coord findEmptyGroundTile(int objSize) {
		Gob player = getPlayer();
		Map scene = getScene(4);
		MapView mv = getWidget(MapView.class);
		scene.initScene(mv, player, null, doAreaList(50.0d));
		Coord tile = scene.findEmptyGroundTile(toSceneCoord(player.getc()), objSize);
		return (tile != null) ? fromSceneCoord(tile) : null;
	}
	
	
	public Coord findRandomWaterTile() {
		Gob player = getPlayer();
		Map scene = getScene(26);
		MapView mv = getWidget(MapView.class);
		scene.initScene(mv, player, null, doAreaList(50.0d));
		Coord wt = scene.findRandomWaterTile(toSceneCoord(player.getc()));
		return (wt != null) ? fromSceneCoord(wt) : null;
	}
	
	public Coord findNextShallowTile(Coord currentPos, Coord prevPos)  {
		Gob player = getPlayer();
		Map scene = getScene(26);
		MapView mv = getWidget(MapView.class);
		scene.initScene(mv, player, null, doAreaList(50.0d));
		Coord st = scene.findNextShallowTile(toSceneCoord(currentPos), toSceneCoord(prevPos));
		return (st != null) ? fromSceneCoord(st) : null;
	}
	
	public Coord findShore() {
		Gob player = getPlayer();
		Map scene = getScene(26);
		MapView mv = getWidget(MapView.class);
		scene.initScene(mv, player, null, doAreaList(50.0d));
		Coord shore = scene.findClosestShoreTile(toSceneCoord(player.getc()));
		return (shore != null) ? fromSceneCoord(shore) : null;
	}
}