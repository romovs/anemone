package haven.event;

public interface PackListener extends MaidEventListener {
	void onPackExecute(PackEvent packEvent);
}