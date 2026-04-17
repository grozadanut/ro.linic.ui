package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionTransaction {
	Active_Single_transaction_in_receipt("1"), Inactive_default_value("0");

	private final String value;

	private OptionTransaction(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
