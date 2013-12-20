package haven.event;

public class MovementEvent extends MaidEvent {
	private Object value;

	public MovementEvent() {
	}

	public MovementEvent(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}
}
