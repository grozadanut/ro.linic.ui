package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionFiscalReceiptPrintType {
	Buffered_Printing("4"), Postponed_printing("2"), Step_by_step_printing("0");

	private final String value;

	private OptionFiscalReceiptPrintType(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
