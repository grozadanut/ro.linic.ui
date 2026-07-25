package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionDecimalPointPosition {
        Fractions("2"),
        Whole_numbers("0");

        private final String value;
        private OptionDecimalPointPosition(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
