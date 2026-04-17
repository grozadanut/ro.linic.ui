package ro.linic.ui.pos.driver.zfplab.internal;
public class CustomerVATNoRes {
   /**
    *15 symbols for VAT number
    */
    public String CustomerVATNo;
    public String getCustomerVATNo() {
       return CustomerVATNo;
    }
    protected void setCustomerVATNo(final String value) {
       CustomerVATNo = value;
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
