package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionSign {
        Correction("-"),
        Sale("+");

        private final String value;
        private OptionSign(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
