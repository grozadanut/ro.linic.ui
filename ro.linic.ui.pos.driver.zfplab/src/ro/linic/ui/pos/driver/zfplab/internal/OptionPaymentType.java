package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionPaymentType {
	Payment_0("0"), Payment_1("1"), Payment_2("2"), Payment_3("3"), Payment_4("4"), Payment_5("5"), Payment_6("6"),
	Payment_7("7"), Payment_8("8"), Payment_9("9");

	private final String value;

	private OptionPaymentType(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
