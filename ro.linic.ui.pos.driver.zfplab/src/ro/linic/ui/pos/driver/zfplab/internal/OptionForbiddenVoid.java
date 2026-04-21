package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionForbiddenVoid {
	allowed("0"), forbidden("1");

	private final String value;

	private OptionForbiddenVoid(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
