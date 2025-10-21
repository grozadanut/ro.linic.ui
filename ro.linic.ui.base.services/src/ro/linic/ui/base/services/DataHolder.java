package ro.linic.ui.base.services;

import java.util.List;

import ca.odell.glazedlists.EventList;

public interface DataHolder<T> {
	EventList<T> getData();
	DataHolder<T> clear();
	DataHolder<T> setData(List<T> data);
	DataHolder<T> replace(T source, T target);
	DataHolder<T> remove(List<T> toRemove);
	DataHolder<T> add(T toAdd);
}
