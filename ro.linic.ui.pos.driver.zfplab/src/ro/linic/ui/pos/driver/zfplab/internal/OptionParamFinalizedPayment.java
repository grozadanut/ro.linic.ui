package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionParamFinalizedPayment {
	finalized_payment("0"), not_finalized_payment("1");

	private final String value;

	private OptionParamFinalizedPayment(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
