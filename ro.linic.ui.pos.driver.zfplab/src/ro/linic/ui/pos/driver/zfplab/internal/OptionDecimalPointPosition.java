package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionDecimalPointPosition {
	fractions("2"), whole_numbers("0");

	private final String value;

	private OptionDecimalPointPosition(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
