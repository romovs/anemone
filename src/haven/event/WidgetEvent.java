package haven.event;

import haven.Widget;

public class WidgetEvent<E extends Widget> extends MaidEvent {
	public enum Type {
		CREATE, DESTROY
	}
	
	private Type type;
	private E widget;

	public WidgetEvent(Type type, E widget) {
		this.type = type;
		this.widget = widget;
	}

	public Type getType() {
		return type;
	}

	public E getWidget() {
		return widget;
	}
}
