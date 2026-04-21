package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionSign {
	Correction("-"),
	Sale("+");

	private final String value;
	private OptionSign(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
