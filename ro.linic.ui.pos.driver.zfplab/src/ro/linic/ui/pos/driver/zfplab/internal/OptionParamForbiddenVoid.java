package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionParamForbiddenVoid {
	allowed("0"), forbidden("1");

	private final String value;

	private OptionParamForbiddenVoid(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
