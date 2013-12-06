package haven;

public class SessionData {

    private Thread thread;
    private UI ui;
	public CharWnd charWnd;
	public BuddyWnd buddyWnd;
	private Tex avatar = null;
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
    	return avatar;
    }
    
    public void setAvatar(Avaview av) {
    	if (av != null) {
		    Gob gob = ui.sess.glob.oc.getgob(av.avagob);
		    Avatar ava = null;
		    if(gob != null)
		    	ava = gob.getattr(Avatar.class);
		    if(av != null)
		    	avatar = ava.rend;	
    	}
    }
}
