package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionAutoOpenDrawer {
        No("0"),
        Yes("1");

        private final String value;
        private OptionAutoOpenDrawer(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
