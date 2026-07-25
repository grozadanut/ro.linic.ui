package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionProfileType {
        Profile_0("0"),
        Profile_1("1");

        private final String value;
        private OptionProfileType(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
