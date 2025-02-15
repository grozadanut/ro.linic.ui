package ro.linic.ui.pos.base.services.impl;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.services.ProductDataHolder;

@Component
public class ProductDataHolderImpl implements ProductDataHolder {
	private EventList<Product> data = GlazedLists.threadSafeList(GlazedLists.eventListOf());
	
	@Override
	public EventList<Product> getData() {
		return data;
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
	public void setData(final List<Product> data) {
		this.data.getReadWriteLock().writeLock().lock();
		try {
			this.data.clear();
			this.data.addAll(data);
		} finally {
			this.data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void replace(final Product source, final Product target) {
		data.getReadWriteLock().writeLock().lock();
		try {
			if (data.contains(source))
				data.set(data.indexOf(source), target);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void remove(final List<Product> toRemove) {
		data.getReadWriteLock().writeLock().lock();
		try {
			data.removeAll(toRemove);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void add(final Product toAdd) {
		data.getReadWriteLock().writeLock().lock();
		try {
			data.add(toAdd);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}
}
