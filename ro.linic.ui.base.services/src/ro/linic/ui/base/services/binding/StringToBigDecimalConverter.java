package ro.linic.ui.base.services.binding;

import static ro.flexbiz.util.commons.NumberUtils.isNumeric;
import static ro.flexbiz.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.flexbiz.util.commons.StringUtils.isEmpty;

import java.math.BigDecimal;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Text;

import ro.flexbiz.util.commons.calculator.Calculator;

public class StringToBigDecimalConverter implements IConverter<String, BigDecimal> {
	/**
	 * Calculates the value and sets the result to the swt.Text control 
	 * on ENTER or = key press
	 */
	public static void applyCalculationOnKeyPress(final Text control) {
		control.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.character == '=' || e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					final String expr = control.getText();
					if (!isEmpty(expr) && !isNumeric(expr)) {
						control.setText(Calculator.parse(expr.replaceAll("=", EMPTY_STRING)).toString());
						control.setSelection(control.getText().length());
						e.doit = false;
					}
				}
			}
		});
	}
	
	@Override
	public Object getFromType() {
		return String.class;
	}

	@Override
	public Object getToType() {
		return BigDecimal.class;
	}

	@Override
	public BigDecimal convert(final String fromObject) {
		return Calculator.parse(fromObject);
	}
}
