package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionQtyType {
	for_availability_of_PLU_stock_is_not_monitored("0"), for_disable_negative_quantity("1"),
	for_enable_negative_quantity("2");

	private final String value;

	private OptionQtyType(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
