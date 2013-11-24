package haven.event;

public class MeterEvent extends MaidEvent {
	public enum Type {
		HP,
		STAMINA,
		HUNGER,
		HAPINESS,
		AUTHORITY
	}

	private Type type;
	private int[] values;
	public MeterEvent(Type type, int[] values) {
		this.type = type;
		this.values = values;
	}

	public Type getType() {
		return type;
	}

	public int[] getValues() {
		return values;
	}
	
}
