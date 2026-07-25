package ro.linic.ui.pos.driver.zfplab.internal;
    public enum OptionFooterLine {
        Footer_1("1"),
        Footer_2("2"),
        Footer_3("3");

        private final String value;
        private OptionFooterLine(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
