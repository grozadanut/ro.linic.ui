package ro.linic.ui.base.services;

import java.util.List;

import ca.odell.glazedlists.EventList;

public interface DataHolder<T> {
	EventList<T> getData();
	void clear();
	void setData(List<T> data);
	void replace(T source, T target);
	void remove(List<T> toRemove);
	void add(T toAdd);
}
