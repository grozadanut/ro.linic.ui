package ro.linic.ui.p2.internal.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.query.Collector;

/**
 * Wraps query results inside corresponding UI elements
 */
public abstract class ElementWrapper {

	private Collection<Object> collection = null;
	protected IEclipseContext ctx;

	public ElementWrapper(final IEclipseContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * Transforms a collector returned by a query to a collection
	 * of UI elements
	 */
	public Collection<?> getElements(final Collector<?> collector) {
		collection = new ArrayList<>(collector.size());
		final Iterator<?> iter = collector.iterator();
		while (iter.hasNext()) {
			final Object o = iter.next();
			if (shouldWrap(o))
				collection.add(wrap(o));
		}
		return getCollection();
	}

	/**
	 * Gets the collection where the elements are being stored.
	 */
	protected Collection<?> getCollection() {
		return collection == null ? Collections.emptyList() : collection;
	}

	/**
	 * Determines if this object should be accepted and wrapped
	 * by a UI element.
	 */
	protected boolean shouldWrap(final Object o) {
		return true;
	}

	/**
	 * Wraps a single element of the query result inside a UI element.
	 */
	protected abstract Object wrap(Object item);
}
