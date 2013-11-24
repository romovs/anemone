package haven.event;

public class CursorEvent extends MaidEvent {
	private String name;

	public CursorEvent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
