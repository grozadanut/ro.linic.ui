package ro.linic.ui.base.services.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ro.linic.ui.base.services.GenericDataHolder;
import ro.linic.ui.base.services.model.GenericValue;

public class GenericDataHolderImpl implements GenericDataHolder {
	private EventList<GenericValue> data = GlazedLists.threadSafeList(GlazedLists.eventListOf());
	
	@Override
	public EventList<GenericValue> getData() {
		return data;
	}
	
	@Override
	public void addOrUpdate(final List<GenericValue> targetData, final String targetPrimaryKey, final String sourcePrimaryKey,
			final Map<String, String> targetToHolderKey) {
		this.data.getReadWriteLock().writeLock().lock();
		try {
			final Map<String, GenericValue> dataById = data.stream()
					.collect(Collectors.toMap(gv -> gv.getString(sourcePrimaryKey), Function.identity()));
			
			for (final GenericValue target : targetData) {
				final String targetId = target.getString(targetPrimaryKey);
				if (dataById.containsKey(targetId)) {
					final GenericValue source = dataById.get(targetId);
					source.silenceListeners(true); // speed fix
					source.update(target, targetToHolderKey);
					source.silenceListeners(false);
				}
				else
					data.add(target.clone(targetToHolderKey));
			}
		} finally {
			this.data.getReadWriteLock().writeLock().unlock();
		}
	}
	
	@Override
	public void clear() {
		this.data.getReadWriteLock().writeLock().lock();
		try {
			this.data.clear();
		} finally {
			this.data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void setData(final List<GenericValue> data) {
		this.data.getReadWriteLock().writeLock().lock();
		try {
			this.data.clear();
			this.data.addAll(data);
		} finally {
			this.data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void replace(final GenericValue source, final GenericValue target) {
		data.getReadWriteLock().writeLock().lock();
		try {
			if (data.contains(source))
				data.set(data.indexOf(source), target);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void remove(final List<GenericValue> toRemove) {
		data.getReadWriteLock().writeLock().lock();
		try {
			data.removeAll(toRemove);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void add(final GenericValue toAdd) {
		data.getReadWriteLock().writeLock().lock();
		try {
			data.add(toAdd);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}
}
