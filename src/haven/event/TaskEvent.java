package haven.event;

public class TaskEvent extends MaidEvent {

	private int value;

	public TaskEvent(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public boolean hasStarted() {
		return value == 0;
	}

	public boolean hasFinished() {
		return value == 100;
	}
}
