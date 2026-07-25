package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionFinalizedPayment {
        finalized_payment("0"),
        not_finalized_payment("1");

        private final String value;
        private OptionFinalizedPayment(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
