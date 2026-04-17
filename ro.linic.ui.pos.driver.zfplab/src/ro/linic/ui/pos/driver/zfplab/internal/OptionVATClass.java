package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionVATClass {
	Alte_taxe("F"), VAT_class_A("A"), VAT_class_B("B"), VAT_class_C("C"), VAT_class_D("D"), VAT_class_E("E");

	private final String value;

	private OptionVATClass(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
