package ro.linic.ui.base.services;

import java.util.List;

import ca.odell.glazedlists.EventList;

public interface DataHolder<T> {
	EventList<T> getData();
	void setData(List<T> data);
	void replace(T source, T target);
	void remove(T toRemove);
	void add(T toAdd);
}
