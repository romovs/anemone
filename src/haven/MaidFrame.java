package haven;

import java.awt.Component;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MaidFrame extends MainFrame implements KeyListener {

    private static List<SessionData> sessions = new ArrayList<SessionData>();
    private static int index;
    private Maid maid;

    public MaidFrame(int w, int h) {
        super(w, h);

        Console.setscmd("bot", new Console.Command() {
            public void run(Console cons, String[] args) {
            	String[] maidArgs = null;
            	if (args.length > 2)
            		maidArgs = Arrays.copyOfRange(args, 2, args.length);
                maid.doTask(args[1], maidArgs);
            }
        });
    }
    
    public synchronized static SessionData getCurrentSession() {
    	return (index < sessions.size()) ? sessions.get(index) : null;
    }

    private void addSession() {
    	sessions.get(0).sb.visible = true;
    	
        Thread t = new HackThread(new Runnable() {

            public void run() {
                try {
                    while (true) {
                    	UI loginUi = p.newui(maid, null);
                    	
                    	// add login UI so we can switch to it
                    	// once the session has been established we will need to replace it with proper UI
                    	SessionData loginThreadUi = new SessionData(Thread.currentThread(), loginUi);
                    	addSession(loginThreadUi);
                		MainFrame.instance.setTitle(null);
                        p.ui = loginUi;
                        loginUi.newwidget(SessionBar.ID, "sessionbar", SessionBar.initPos, 0);
                        
                        Bootstrap bill = new Bootstrap();
                        if (Config.defserv != null) {
                            bill.setaddr(Config.defserv);
                        }
                        if ((Config.authuser != null) && (Config.authck != null)) {
                            bill.setinitcookie(Config.authuser, Config.authck);
                            Config.authck = null;
                        }
                        
                        Session sess = bill.run(p, loginUi);
                        RemoteUI rui = new RemoteUI(sess);
                        UI n = p.newui(maid, sess);
                        replaceSession(loginThreadUi, new SessionData(Thread.currentThread(), n));
                        rui.run(n);
                    }
                } catch (InterruptedException e) {
                }
            }
        }, "Haven alternate thread");

        t.start();
    }
    
    
    private synchronized static void addSession(SessionData ses) {
    	sessions.add(ses);
        index = sessions.size() - 1;
    }
    
    private synchronized static void replaceSession(SessionData ses, SessionData newSes) {
        sessions.remove(ses);
        sessions.add(newSes);
    }

    private synchronized static void nextSession() {
        index = (index + 1) % sessions.size();
        ((MaidFrame)MainFrame.instance).switchSession();
    }

    private synchronized static void previousSession() {
        index = index == 0 ? sessions.size() - 1 : index - 1;
        ((MaidFrame)MainFrame.instance).switchSession();
    }

    private synchronized static void firstSession() {
        index = 0;
        ((MaidFrame)MainFrame.instance).switchSession();
    }

    private synchronized static void lastSession() {
        index = sessions.size() - 1;
        ((MaidFrame)MainFrame.instance).switchSession();
    }

    private void switchSession() {
        UI newUi = sessions.get(index).getUI();
        UI.instance = newUi;
        p.ui = newUi;
		MainFrame.instance.setTitle(p.ui.sess != null ? p.ui.sess.charname : null);
    }
    

    public synchronized static void switchToSession(int index) {
    	if (index != MaidFrame.index) {
    		MaidFrame.index = index;
    		((MaidFrame)MainFrame.instance).switchSession();
    	}
    }

    // This is not synchronized!!! Extra care should be taken when using!
    public static List<SessionData> getSessionList() {
    	return sessions;
    }
    
    public static int getSessionCount() {
    	return sessions.size();
    }
    
    public synchronized static void removeSession(UI ui) {
    	int i;
    	for (i = 0; i < sessions.size(); i++) {
    		if (sessions.get(i).getUI() == ui)
    			break;
        }
    	if (i < sessions.size())
    		sessions.remove(i);
    }

    @Override
    public Component add(Component comp) {
        if (comp instanceof HavenPanel) {
            comp.addKeyListener(this);

            if (maid == null) {
                maid = new Maid();
            }
            maid.setHaven((HavenPanel) comp);
        }

        return super.add(comp);
    }

    @Override
    public void remove(Component comp) {
        if (comp instanceof HavenPanel) {
            comp.removeKeyListener(this);

            maid = null;
        }
        super.remove(comp);
    }

    public void keyPressed(KeyEvent e) {
        if (e.isShiftDown()) {
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.isShiftDown()) {
            int i;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_F1:
                    i = 0;
                    break;
                case KeyEvent.VK_F2:
                    i = 1;
                    break;
                case KeyEvent.VK_F3:
                    i = 2;
                    break;
                case KeyEvent.VK_F4:
                    i = 3;
                    break;
                case KeyEvent.VK_F5:
                    i = 4;
                    break;
                case KeyEvent.VK_F6:
                    i = 5;
                    break;
                case KeyEvent.VK_F7:
                    i = 6;
                    break;
                case KeyEvent.VK_F8:
                    i = 7;
                    break;
                case KeyEvent.VK_F9:
                    i = 8;
                    break;
                case KeyEvent.VK_F10:
                    i = 9;
                    break;
                case KeyEvent.VK_F11:
                    i = 10;
                    break;
                case KeyEvent.VK_F12:
                    i = 11;
                    break;
                case KeyEvent.VK_ESCAPE:
                    i = -2;
                    break;
                default:
                    return;
            }
            e.consume();
            if (i >= 0) {
                maid.doTask(i);
            } else if (i == -2) {
                maid.stopTask();
            }
        } else if (e.isAltDown()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_INSERT:
                    addSession();
                    break;
                case KeyEvent.VK_PAGE_UP:
                    nextSession();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    previousSession();
                    break;
                case KeyEvent.VK_HOME:
                    firstSession();
                    break;
                case KeyEvent.VK_END:
                    lastSession();
                    break;
                default:
                    return;
            }

            e.consume();
        }
    }

    public void keyTyped(KeyEvent e) {
        if (e.isShiftDown()) {
            e.consume();
        }
    }

   // @Override
    public void run() {
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                g.interrupt();
            }
        });
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent evt) {
                innerSize.setSize(getWidth() - insetsSize.width, getHeight() - insetsSize.height);
                centerPoint.setLocation(innerSize.width / 2, innerSize.height / 2);
            }
        });
        Thread ui = new HackThread(p, "Haven UI thread");
        p.setfsm(this);
        ui.start();
        try {
            while (true) {         	
            	UI loginUi = p.newui(maid, null);
            	
            	// add login UI so we can switch to it
            	// once the session has been established we will need to replace it with proper UI
            	SessionData loginThreadUi = new SessionData(Thread.currentThread(), loginUi);
            	addSession(loginThreadUi);
                p.ui = loginUi;
            	
                Bootstrap bill = new Bootstrap();
                if (Config.defserv != null) {
                    bill.setaddr(Config.defserv);
                }
                if ((Config.authuser != null) && (Config.authck != null)) {
                    bill.setinitcookie(Config.authuser, Config.authck);
                    Config.authck = null;
                }
                
                Session sess = bill.run(p, loginUi);
                RemoteUI rui = new RemoteUI(sess);
                UI n = p.newui(maid, sess);
                replaceSession(loginThreadUi, new SessionData(Thread.currentThread(), n));

                rui.run(n);
            }
        } catch (InterruptedException e) {
        } finally {
            ui.interrupt();
            dispose();
        }
    }

    public static void main(final String[] args) {
        /*
         * Set up the error handler as early as humanly possible.
         */
        ThreadGroup g = new ThreadGroup("Haven client");
        String ed;
        if (!(ed = Utils.getprop("haven.errorurl", "")).equals("")) {
            try {
                final haven.error.ErrorHandler hg = new haven.error.ErrorHandler(new java.net.URL(ed));
                hg.sethandler(new haven.error.ErrorGui(null) {

                    public void errorsent() {
                        hg.interrupt();
                    }
                });
                g = hg;
            } catch (java.net.MalformedURLException e) {
            }
        }
        Thread main = new HackThread(g, new Runnable() {

            public void run() {
                try {
                    javabughack();
                } catch (InterruptedException e) {
                    return;
                }
                main2(args);
            }
        }, "Haven main thread");
        main.start();
        try {
            main.join();
        } catch (InterruptedException e) {
            g.interrupt();
            return;
        }
        System.exit(0);
    }

    private static void javabughack() throws InterruptedException {
        /*
         * Work around a stupid deadlock bug in AWT.
         */
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    PrintStream bitbucket = new PrintStream(new ByteArrayOutputStream());
                    bitbucket.print(LoginScreen.textf);
                    bitbucket.print(LoginScreen.textfs);
                }
            });
        } catch (java.lang.reflect.InvocationTargetException e) {
            /*
             * Oh, how I love Swing!
             */
            throw (new Error(e));
        }
        /*
         * Work around another deadl bug in Sun's JNLP client.
         */
        javax.imageio.spi.IIORegistry.getDefaultInstance();
    }

    private static void main2(String[] args) {
        Config.cmdline(args);
        ThreadGroup g = HackThread.tg();
        setupres();
        MaidFrame f = new MaidFrame(800, 600);
        if (Config.fullscreen) {
            f.setfs();
        }
        f.g = g;
        if (g instanceof haven.error.ErrorHandler) {
            final haven.error.ErrorHandler hg = (haven.error.ErrorHandler) g;
            hg.sethandler(new haven.error.ErrorGui(null) {

                public void errorsent() {
                    hg.interrupt();
                }
            });
        }
        f.run();
        dumplist(Resource.loadwaited, Config.loadwaited);
        dumplist(Resource.cached(), Config.allused);
        if (ResCache.global != null) {
            try {
                Collection<Resource> used = new LinkedList<Resource>();
                for (Resource res : Resource.cached()) {
                    if (res.prio >= 0) {
                        used.add(res);
                    }
                }
                Writer w = new OutputStreamWriter(ResCache.global.store("tmp/allused"), "UTF-8");
                try {
                    Resource.dumplist(used, w);
                } finally {
                    w.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private static void dumplist(Collection<Resource> list, String fn) {
        try {
            if (fn != null) {
                Writer w = new OutputStreamWriter(new FileOutputStream(fn), "UTF-8");
                try {
                    Resource.dumplist(list, w);
                } finally {
                    w.close();
                }
            }
        } catch (IOException e) {
            throw (new RuntimeException(e));
        }
    }
}
