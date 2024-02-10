package ro.linic.ui.legacy.tables.components;

import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.extractPercentage_EmptyAsNull;
import static ro.colibri.util.PresentationUtils.displayPercentage;

import java.math.BigDecimal;

import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;

public class PercentageBigDecimalDisplayConverter extends DisplayConverter
{
	boolean emptyAsNull = false;
	
	public PercentageBigDecimalDisplayConverter()
	{
	}
	
	public PercentageBigDecimalDisplayConverter(final boolean emptyAsNull)
	{
		this.emptyAsNull = emptyAsNull;
	}
	
	@Override
	public Object canonicalToDisplayValue(final Object canonicalValue)
	{
		if (canonicalValue instanceof BigDecimal)
			return displayPercentage((BigDecimal) canonicalValue);
		
		return ""; //$NON-NLS-1$
	}

	@Override
	public Object displayToCanonicalValue(final Object displayValue)
	{
		String displayString = (String) displayValue;
		displayString = displayString.trim();
		if (displayString.endsWith("%")) //$NON-NLS-1$
		{
			displayString = displayString.substring(0, displayString.length() - 1);
		}
		displayString = displayString.trim();
		return emptyAsNull ? extractPercentage_EmptyAsNull(displayString) : extractPercentage(displayString);
	}
}
