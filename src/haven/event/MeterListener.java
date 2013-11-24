package haven.event;

public interface MeterListener extends MaidEventListener {
	void onHealChange(MeterEvent meterEvent);

	void onStaminaChange(MeterEvent meterEvent);

	void onHungerChange(MeterEvent meterEvent);

	void onHappinessChange(MeterEvent meterEvent);

	void onAuthorityChange(MeterEvent meterEvent);
}
