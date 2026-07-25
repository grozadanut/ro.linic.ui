package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionUsedModule {
        LAN("1"),
        WiFi("2");

        private final String value;
        private OptionUsedModule(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
