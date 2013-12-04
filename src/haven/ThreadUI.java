package haven;

public class ThreadUI {

    private Thread thread;
    private UI ui;
	public CharWnd charWnd;
	public BuddyWnd buddyWnd;
	
    public ThreadUI(Thread thread, UI ui) {
        this.thread = thread;
        this.ui = ui;
    }

    public Thread getThread() {
        return thread;
    }

    public UI getUi() {
        return ui;
    }
}
