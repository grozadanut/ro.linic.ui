package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionQTY {
	Availability_of_PLU_stock_is_not_monitored("0"), Disable_Negative_Quantity("1"), Enable_Negative_Quantity("2");

	private final String value;

	private OptionQTY(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
