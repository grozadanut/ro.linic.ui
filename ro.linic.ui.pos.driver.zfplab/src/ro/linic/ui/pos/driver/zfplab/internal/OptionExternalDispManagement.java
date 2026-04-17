package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionExternalDispManagement {
	Auto("0"), Manuel("1");

	private final String value;

	private OptionExternalDispManagement(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
