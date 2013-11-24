package haven.event;

import haven.Item;

public class ItemEvent extends MaidEvent {
	public enum Type {
		GRAB, RELEASE, CREATE, DESTROY,
	}
	private Type type;
	private Item item;
	
	public ItemEvent(Type type, Item item) {
		this.type = type;
		this.item = item;
	}

	public Type getType() {
		return type;
	}

	public Item getItem() {
		return item;
	}
	
}
