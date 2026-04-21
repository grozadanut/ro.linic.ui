package ro.linic.ui.pos.driver.zfplab.internal;
public class CustomerVATNumRes {
   /**
    *15 symbols for VAT number
    */
    public String CustomerVATNum;
    public String getCustomerVATNum() {
       return CustomerVATNum;
    }
    protected void setCustomerVATNum(final String value) {
       CustomerVATNum = value;
    }

   /**
    *1 symbol for type of owner's VAT registration: 
    * - '1' - Yes 
    * - '0' - No
    */
    public OptionTypeVATregistration OptionTypeVATregistration;
    public OptionTypeVATregistration getOptionTypeVATregistration() {
       return OptionTypeVATregistration;
    }
    protected void setOptionTypeVATregistration(final OptionTypeVATregistration value) {
       OptionTypeVATregistration = value;
    }
}
