package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionFlagPowerDown {
	no_power_down("0"), power_down("1");

	private final String value;

	private OptionFlagPowerDown(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
