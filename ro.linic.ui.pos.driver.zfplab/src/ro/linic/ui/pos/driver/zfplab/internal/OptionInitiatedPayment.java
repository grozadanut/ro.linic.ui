package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionInitiatedPayment {
	initiated_payment("0"), not_initiated_payment("1");

	private final String value;

	private OptionInitiatedPayment(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
