package haven.event;

public class MeterEventObjectHunger extends MeterEventObject {
	public enum HungerType {
		STARVING,
		VERY_HUNGRY,
		HUNGRY,
		FULL,
		OVERSTUFFED
	}
	
	public HungerType type;
	
	public MeterEventObjectHunger(HungerType type, int value) {
		this.type = type;
		this.value = value;
	}
}
