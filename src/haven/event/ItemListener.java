package haven.event;

public interface ItemListener extends MaidEventListener {
	void onItemCreate(ItemEvent e);
	void onItemGrab(ItemEvent e);
	void onItemRelease(ItemEvent e);
	void onItemDestroy(ItemEvent e);
}
