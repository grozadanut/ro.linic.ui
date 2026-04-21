package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionReceiptFormat {
	Brief("0"),
	Detailed("1");

	private final String value;
	private OptionReceiptFormat(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
