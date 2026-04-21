package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionFinalizedPayment {
	finalized_payment("0"), not_finalized_payment("1");

	private final String value;

	private OptionFinalizedPayment(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
