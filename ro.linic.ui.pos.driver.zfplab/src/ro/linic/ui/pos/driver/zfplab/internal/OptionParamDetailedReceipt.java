package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionParamDetailedReceipt {
	brief("0"), detailed_format("1");

	private final String value;

	private OptionParamDetailedReceipt(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
