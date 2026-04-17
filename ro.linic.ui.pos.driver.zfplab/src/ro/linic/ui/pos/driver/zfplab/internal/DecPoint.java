package ro.linic.ui.pos.driver.zfplab.internal;

public enum DecPoint {
	fractions("2"), whole_numbers("0");

	private final String value;

	private DecPoint(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
