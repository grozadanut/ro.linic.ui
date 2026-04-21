package ro.linic.ui.pos.driver.zfplab.internal;
public class ParametersRes {
   /**
    *(POS No) 4 symbols for number of POS in format ####
    */
    public Double POSNum;
    public Double getPOSNum() {
       return POSNum;
    }
    protected void setPOSNum(final Double value) {
       POSNum = value;
    }

   /**
    *(Print Logo) 1 symbol of value: 
    * - '1' - Yes 
    * - '0' - No
    */
    public OptionPrintLogo OptionPrintLogo;
    public OptionPrintLogo getOptionPrintLogo() {
       return OptionPrintLogo;
    }
    protected void setOptionPrintLogo(final OptionPrintLogo value) {
       OptionPrintLogo = value;
    }

   /**
    *(Auto Open Drawer) 1 symbol of value: 
    * - '1' - Yes 
    * - '0' - No
    */
    public OptionAutoOpenDrawer OptionAutoOpenDrawer;
    public OptionAutoOpenDrawer getOptionAutoOpenDrawer() {
       return OptionAutoOpenDrawer;
    }
    protected void setOptionAutoOpenDrawer(final OptionAutoOpenDrawer value) {
       OptionAutoOpenDrawer = value;
    }

   /**
    *(Auto Cut) 1 symbol of value: 
    * - '1' - Yes 
    * - '0' - No
    */
    public OptionAutoCut OptionAutoCut;
    public OptionAutoCut getOptionAutoCut() {
       return OptionAutoCut;
    }
    protected void setOptionAutoCut(final OptionAutoCut value) {
       OptionAutoCut = value;
    }

   /**
    *(External Display Management) 1 symbol of value: 
    * - '1' - Manuel 
    * - '0' - Auto
    */
    public OptionExternalDispManagement OptionExternalDispManagement;
    public OptionExternalDispManagement getOptionExternalDispManagement() {
       return OptionExternalDispManagement;
    }
    protected void setOptionExternalDispManagement(final OptionExternalDispManagement value) {
       OptionExternalDispManagement = value;
    }

   /**
    *(Enable Currency) 1 symbol of value: 
    * - '1' - Yes 
    * - '0' - No
    */
    public OptionEnableCurrency OptionEnableCurrency;
    public OptionEnableCurrency getOptionEnableCurrency() {
       return OptionEnableCurrency;
    }
    protected void setOptionEnableCurrency(final OptionEnableCurrency value) {
       OptionEnableCurrency = value;
    }

   /**
    *(USB in host mode)1 symbol with value: 
    * - '1' - Yes 
    * - '0' - No
    */
    public OptionUSBHost OptionUSBHost;
    public OptionUSBHost getOptionUSBHost() {
       return OptionUSBHost;
    }
    protected void setOptionUSBHost(final OptionUSBHost value) {
       OptionUSBHost = value;
    }
}
