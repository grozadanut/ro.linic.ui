package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionClientReceipt {
        invoice_client_receipt("1"),
        standard_receipt("0");

        private final String value;
        private OptionClientReceipt(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
