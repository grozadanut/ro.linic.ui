package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;

public class ImageDisplayConverter extends DisplayConverter
{
	@Override
	public Object canonicalToDisplayValue(final Object sourceValue)
	{
		return sourceValue; //$NON-NLS-1$
	}

	@Override
	public Object displayToCanonicalValue(final Object destinationValue)
	{
		if (destinationValue == null || destinationValue.toString().length() == 0)
			return null;
		else
			return destinationValue;
	}
}