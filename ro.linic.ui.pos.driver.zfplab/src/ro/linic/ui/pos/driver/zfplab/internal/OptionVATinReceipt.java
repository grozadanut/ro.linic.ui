package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionVATinReceipt {
	with_printing("0"),
	without_printing("1");

	private final String value;
	private OptionVATinReceipt(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
