package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionType {
	Defined_from_the_device("2"), Over_subtotal("1"), Over_transaction_sum("0");

	private final String value;

	private OptionType(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
