package haven.event;


public interface MovementListener extends MaidEventListener {
	void onMovementStart(MovementEvent moveEvent);

	void onMovementStop(MovementEvent moveEvent);
}