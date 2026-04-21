package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionDecimalPointPosition {
	Fractions("2"), Whole_numbers("0");

	private final String value;

	private OptionDecimalPointPosition(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
