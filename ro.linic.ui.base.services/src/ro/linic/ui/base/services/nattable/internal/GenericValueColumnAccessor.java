package ro.linic.ui.base.services.nattable.internal;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;

import ro.linic.ui.base.services.model.GenericValue;

public class GenericValueColumnAccessor<R> extends ReflectiveColumnPropertyAccessor<R> {
	public GenericValueColumnAccessor(final String... propertyNames) {
        super(propertyNames);
    }
	
    public GenericValueColumnAccessor(final List<String> propertyNames) {
    	super(propertyNames);
    }
    
    @Override
    public Object getDataValue(final R rowObj, final int columnIndex) {
    	if (rowObj instanceof GenericValue) {
    		final String columnProperty = getColumnProperty(columnIndex);
    		return ((GenericValue) rowObj).get(columnProperty);
    	}
    	
        return super.getDataValue(rowObj, columnIndex);
    }

    @Override
    public void setDataValue(final R rowObj, final int columnIndex, final Object newValue) {
    	if (rowObj instanceof GenericValue) {
    		final String columnProperty = getColumnProperty(columnIndex);
    		((GenericValue) rowObj).put(columnProperty, newValue);
    	} else {
    		super.setDataValue(rowObj, columnIndex, newValue);
    	}
    }
}
