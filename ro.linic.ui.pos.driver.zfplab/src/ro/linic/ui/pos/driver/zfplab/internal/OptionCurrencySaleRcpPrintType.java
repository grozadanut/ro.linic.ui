package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionCurrencySaleRcpPrintType {
        Postponed_printing("2"),
        Step_by_step_printing("0");

        private final String value;
        private OptionCurrencySaleRcpPrintType(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
