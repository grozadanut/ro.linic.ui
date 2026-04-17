package ro.linic.ui.pos.driver.zfplab.internal;

public enum OptionRcpXmlStorage {
	Storage_in_External_SD_card_memory("JX"), Storage_in_External_USB_Flash_memory("Jx");

	private final String value;

	private OptionRcpXmlStorage(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
