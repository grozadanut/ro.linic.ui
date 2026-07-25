package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionReportStorage {
        Printing("J1"),
        Storage_in_External_SD_card_memory("J4"),
        Storage_in_External_USB_Flash_memory("J2");

        private final String value;
        private OptionReportStorage(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
