package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionTCPAutoStart {
	Disable("0"),
	Enable("1");

	private final String value;
	private OptionTCPAutoStart(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
