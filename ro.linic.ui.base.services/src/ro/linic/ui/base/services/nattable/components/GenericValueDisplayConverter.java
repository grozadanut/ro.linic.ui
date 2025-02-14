package ro.linic.ui.base.services.nattable.components;

import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;

import ro.linic.ui.base.services.model.GenericValue;

public class GenericValueDisplayConverter extends DefaultDisplayConverter {
	private final String key;

	public GenericValueDisplayConverter(final String key) {
		this.key = key;
	}

	@Override
	public Object canonicalToDisplayValue(final Object canonicalValue) {
		if (canonicalValue instanceof GenericValue) {
			return ((GenericValue) canonicalValue).get(key);
		}
		return super.canonicalToDisplayValue(canonicalValue);
	}

	@Override
	public Object displayToCanonicalValue(final Object displayValue) {
		if (displayValue == null
                || displayValue.toString().length() == 0) {
            return null;
        } else {
            return GenericValue.of("", "id", key, displayValue);
        }
	}
}
