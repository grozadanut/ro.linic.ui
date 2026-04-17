package ro.linic.ui.pos.driver.zfplab.internal;
public class VATNumberRes {
   /**
    *15 symbols for owner's VAT registration number
    */
    public String VATNo;
    public String getVATNo() {
       return VATNo;
    }
    protected void setVATNo(final String value) {
       VATNo = value;
    }

   /**
    *10 symbols for FM serial number
    */
    public String FMnumber;
    public String getFMnumber() {
       return FMnumber;
    }
    protected void setFMnumber(final String value) {
       FMnumber = value;
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
