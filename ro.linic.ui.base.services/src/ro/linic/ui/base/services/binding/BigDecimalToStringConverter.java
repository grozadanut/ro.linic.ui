package ro.linic.ui.base.services.binding;

import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;

import java.math.BigDecimal;

import org.eclipse.core.databinding.conversion.IConverter;

public class BigDecimalToStringConverter implements IConverter<BigDecimal, String> {
	@Override
	public Object getFromType() {
		return BigDecimal.class;
	}

	@Override
	public Object getToType() {
		return String.class;
	}

	@Override
	public String convert(final BigDecimal fromObject) {
		return fromObject != null ? fromObject.toString() : EMPTY_STRING;
	}
}
