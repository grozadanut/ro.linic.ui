package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionNonFiscalPrintType {
        Postponed_printing("1"),
        Step_by_step_printing("0");

        private final String value;
        private OptionNonFiscalPrintType(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
