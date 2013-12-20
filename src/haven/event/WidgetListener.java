package haven.event;

import haven.Widget;

public interface WidgetListener<E extends Widget> extends MaidEventListener {
	Class<E> getInterest();

	void onCreate(WidgetEvent<E> e);

	void onDestroy(WidgetEvent<E> e);
}