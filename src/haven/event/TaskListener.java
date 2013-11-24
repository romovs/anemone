package haven.event;

public interface TaskListener extends MaidEventListener {
	void onTaskComplete(TaskEvent taskEvent);

	void onTaskProgress(TaskEvent taskEvent);
}