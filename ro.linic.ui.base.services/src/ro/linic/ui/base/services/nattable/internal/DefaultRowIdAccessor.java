package ro.linic.ui.base.services.nattable.internal;

import java.io.Serializable;

import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;

import ro.linic.ui.base.services.nattable.components.IdSupplier;

public class DefaultRowIdAccessor<T> implements IRowIdAccessor<T> {
	@Override
	public Serializable getRowId(final T rowObject) {
		if (rowObject instanceof IdSupplier)
			return ((IdSupplier) rowObject).getId();
		return rowObject.hashCode();
	}
}
