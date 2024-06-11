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
	public void setData(final List<Product> data) {
		try {
			this.data.getReadWriteLock().writeLock().lock();
			this.data.clear();
			this.data.addAll(data);
		} finally {
			this.data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void replace(final Product source, final Product target) {
		try {
			data.getReadWriteLock().writeLock().lock();
			if (data.contains(source))
				data.set(data.indexOf(source), target);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void remove(final List<Product> toRemove) {
		try {
			data.getReadWriteLock().writeLock().lock();
			data.removeAll(toRemove);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}

	@Override
	public void add(final Product toAdd) {
		try {
			data.getReadWriteLock().writeLock().lock();
			data.add(toAdd);
		} finally {
			data.getReadWriteLock().writeLock().unlock();
		}
	}
}
