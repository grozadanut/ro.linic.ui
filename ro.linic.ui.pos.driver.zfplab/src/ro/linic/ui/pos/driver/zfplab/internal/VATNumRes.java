package ro.linic.ui.pos.driver.zfplab.internal;
public class VATNumRes {
   /**
    *15 symbols for owner's VAT registration number
    */
    public String VATNum;
    public String getVATNum() {
       return VATNum;
    }
    protected void setVATNum(final String value) {
       VATNum = value;
    }

   /**
    *10 symbols for FM serial number
    */
    public String FMnum;
    public String getFMnum() {
       return FMnum;
    }
    protected void setFMnum(final String value) {
       FMnum = value;
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
