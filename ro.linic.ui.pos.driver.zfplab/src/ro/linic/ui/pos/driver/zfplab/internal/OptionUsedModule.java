package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionUsedModule {
	LAN_module("1"),
	WiFi_module("2");

	private final String value;
	private OptionUsedModule(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
