package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionCommunicationModule {
        GSM("0"),
        LAN("1");

        private final String value;
        private OptionCommunicationModule(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
