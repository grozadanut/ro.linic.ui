package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionIsReceiptOpened {
	No("0"), Yes("1");

	private final String value;

	private OptionIsReceiptOpened(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
