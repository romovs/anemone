package haven;

public class SessionData {

    private Thread thread;
    private UI ui;
	public CharWnd charWnd;
	public BuddyWnd buddyWnd;
	private Avaview av;
	public SessionBar sb;

    public SessionData(Thread thread, UI ui) {
        this.thread = thread;
        this.ui = ui;
    }

    public Thread getThread() {
        return thread;
    }

    public UI getUI() {
        return ui;
    }
    
    public Tex getAvatar() {
    	if (av != null) {
    	Gob avaGob = ui.sess.glob.oc.getgob(av.avagob);
	    Avatar ava = null;
	    if(avaGob != null)
	    	ava = avaGob.getattr(Avatar.class);
	    if(ava != null)
	    	return ava.rend;	
    	}
    	
    	return null;
    }
    
    public void setAvatar(Avaview av) {
    	this.av = av;
    }
}
