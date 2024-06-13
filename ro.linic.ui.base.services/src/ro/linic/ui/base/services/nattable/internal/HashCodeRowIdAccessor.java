package ro.linic.ui.base.services.nattable.internal;

import java.io.Serializable;

import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;

public class HashCodeRowIdAccessor<T> implements IRowIdAccessor<T> {
	@Override
	public Serializable getRowId(final T rowObject) {
		return rowObject.hashCode();
	}
}
