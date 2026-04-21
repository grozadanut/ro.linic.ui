package ro.linic.ui.base.widgets;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * A field editor for a double type preference.
 */
public class DoubleFieldEditor extends StringFieldEditor {
	private double minValidValue = 0;

	private double maxValidValue = Double.MAX_VALUE;

	private static final int DEFAULT_TEXT_LIMIT = 10;

	/**
	* Creates a new double field editor
	*/
	protected DoubleFieldEditor() {
	}

	/**
	 * Creates an double field editor.
	 *
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent of the field editor's control
	 */
	public DoubleFieldEditor(final String name, final String labelText, final Composite parent) {
		this(name, labelText, parent, DEFAULT_TEXT_LIMIT);
	}

	/**
	 * Creates an double field editor.
	 *
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent of the field editor's control
	 * @param textLimit the maximum number of characters in the text.
	 */
	public DoubleFieldEditor(final String name, final String labelText, final Composite parent,
			final int textLimit) {
		init(name, labelText);
		setTextLimit(textLimit);
		setEmptyStringAllowed(false);
		setErrorMessage(JFaceResources
				.getString("IntegerFieldEditor.errorMessage"));//$NON-NLS-1$
		createControl(parent);
	}

	/**
	 * Sets the range of valid values for this field.
	 *
	 * @param min the minimum allowed value (inclusive)
	 * @param max the maximum allowed value (inclusive)
	 */
	public void setValidRange(final double min, final double max) {
		minValidValue = min;
		maxValidValue = max;
		setErrorMessage(JFaceResources.format("IntegerFieldEditor.errorMessageRange", //$NON-NLS-1$
				Double.valueOf(min), Double.valueOf(max)));
	}

	@Override
	protected boolean checkState() {

		final Text text = getTextControl();

		if (text == null) {
			return false;
		}

		final String numberString = text.getText();
		try {
			final double number = Double.parseDouble(numberString);
			if (number >= minValidValue && number <= maxValidValue) {
				clearErrorMessage();
				return true;
			}

			showErrorMessage();
			return false;

		} catch (final NumberFormatException e1) {
			showErrorMessage();
		}

		return false;
	}

	@Override
	protected void doLoad() {
		final Text text = getTextControl();
		if (text != null) {
			final double value = getPreferenceStore().getDouble(getPreferenceName());
			text.setText(Double.toString(value));
			oldValue = Double.toString(value);
		}

	}

	@Override
	protected void doLoadDefault() {
		final Text text = getTextControl();
		if (text != null) {
			final double value = getPreferenceStore().getDefaultDouble(getPreferenceName());
			text.setText(Double.toString(value));
		}
		valueChanged();
	}

	@Override
	protected void doStore() {
		final Text text = getTextControl();
		if (text != null) {
			final Double i = Double.valueOf(text.getText());
			getPreferenceStore().setValue(getPreferenceName(), i.doubleValue());
		}
	}

	/**
	 * Returns this field editor's current value as an integer.
	 *
	 * @return the value
	 * @exception NumberFormatException if the <code>String</code> does not
	 *   contain a parsable integer
	 */
	public double getDoubleValue() throws NumberFormatException {
		return Double.parseDouble(getStringValue());
	}
}
