package ro.linic.ui.base.services.nattable.components;

import static org.eclipse.nebula.widgets.nattable.util.ObjectUtils.isNotEmpty;
import static org.eclipse.nebula.widgets.nattable.util.ObjectUtils.isNotNull;

import java.math.BigDecimal;

import org.eclipse.nebula.widgets.nattable.Messages;
import org.eclipse.nebula.widgets.nattable.data.convert.ConversionFailedException;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;

import ro.linic.util.commons.calculator.Calculator;

/**
 * Converts the display value to a {@link BigDecimal} and vice versa.
 */
public class CalculatorBigDecimalDisplayConverter extends DisplayConverter {
	@Override
    public Object canonicalToDisplayValue(final Object canonicalValue) {
        try {
            if (isNotNull(canonicalValue))
                return canonicalValue.toString();
            return null;
        } catch (final Exception e) {
            return canonicalValue;
        }
    }

    @Override
    public Object displayToCanonicalValue(final Object displayValue) {
        try {
            if (isNotNull(displayValue) && isNotEmpty(displayValue.toString()))
                return convertToNumericValue(displayValue.toString().trim());
            return null;
        } catch (final Exception e) {
            throw new ConversionFailedException(Messages.getString("NumericDisplayConverter.failure", //$NON-NLS-1$
                    displayValue), e);
        }
    }
    
	private Object convertToNumericValue(final String value) {
		return Calculator.parse(value);
	}
}
