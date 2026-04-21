package ro.linic.ui.pos.driver.zfplab.internal;
public enum OptionVATClass {
	Alte_taxe("F"),
	VAT_Class_A("A"),
	VAT_Class_B("B"),
	VAT_Class_C("C"),
	VAT_Class_D("D"),
	VAT_Class_E("E");

	private final String value;
	private OptionVATClass(final String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return value;
	}
}
