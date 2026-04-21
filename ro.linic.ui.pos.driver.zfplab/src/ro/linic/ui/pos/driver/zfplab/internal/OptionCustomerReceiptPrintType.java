package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionCustomerReceiptPrintType {
	Buffered_printing("5"), Postponed_printing("3"), Step_by_step_printing("1");

	private final String value;

	private OptionCustomerReceiptPrintType(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
