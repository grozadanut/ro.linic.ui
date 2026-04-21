package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionQuantityType {
	Availability_of_PLU_stock_is_not_monitored("0"),
	Disable_negative_quantity("1"),
	Enable_negative_quantity("2");

	private final String value;
	private OptionQuantityType(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
