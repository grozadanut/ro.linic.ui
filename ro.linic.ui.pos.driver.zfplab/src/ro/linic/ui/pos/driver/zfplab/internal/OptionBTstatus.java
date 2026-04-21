package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionBTstatus {
	Disabled("0"), Enabled("1");

	private final String value;

	private OptionBTstatus(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
