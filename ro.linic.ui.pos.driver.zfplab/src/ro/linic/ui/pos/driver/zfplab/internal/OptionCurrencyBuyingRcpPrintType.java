package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionCurrencyBuyingRcpPrintType {
	Postponed_printing(":"), Step_by_step_printing("8");

	private final String value;

	private OptionCurrencyBuyingRcpPrintType(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
