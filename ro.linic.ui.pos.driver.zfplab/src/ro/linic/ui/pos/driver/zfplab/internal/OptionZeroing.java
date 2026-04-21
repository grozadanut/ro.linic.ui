package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionZeroing {
	Not_zeroing("X"),
	Zeroing("Z");

	private final String value;
	private OptionZeroing(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
