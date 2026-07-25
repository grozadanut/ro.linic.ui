package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionCurrencyBuyingRcpPrintType {
        Postponed_printing(":"),
        Step_by_step_printing("8");

        private final String value;
        private OptionCurrencyBuyingRcpPrintType(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
