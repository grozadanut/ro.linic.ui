package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionPaymentType {
	Payment_0("0"), // NUMERAR
	Payment_1("1"), // CARD
	Payment_2("2"),
	Payment_3("3"), // TICHETE
	Payment_4("4"), // BONURI
	Payment_5("5"), // VOUCHER
	Payment_6("6"), // CREDIT
	Payment_7("7"), // MODERNE
	Payment_8("8"), // ALTE
	Payment_9("9");

	private final String value;
	private OptionPaymentType(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
