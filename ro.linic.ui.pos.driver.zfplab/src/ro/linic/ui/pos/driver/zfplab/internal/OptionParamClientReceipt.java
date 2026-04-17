package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionParamClientReceipt {
	invoice_client_receipt("1"), standard_receipt("0");

	private final String value;

	private OptionParamClientReceipt(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
