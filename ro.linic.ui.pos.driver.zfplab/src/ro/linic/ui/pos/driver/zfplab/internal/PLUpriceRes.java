package ro.linic.ui.pos.driver.zfplab.internal;
public class PLUpriceRes {
   /**
    *5 symbols for article number with leading zeroes in format #####
    */
    public Double PLUNum;
    public Double getPLUNum() {
       return PLUNum;
    }
    protected void setPLUNum(final Double value) {
       PLUNum = value;
    }

   /**
    *1..10 symbols for article price
    */
    public Double Price;
    public Double getPrice() {
       return Price;
    }
    protected void setPrice(final Double value) {
       Price = value;
    }

   /**
    *1 byte for Price flag with next value: 
    * - '0'- Free price is disable valid only programmed price 
    * - '1'- Free price is enable 
    * - '2'- Limited price
    */
    public OptionPrice OptionPrice;
    public OptionPrice getOptionPrice() {
       return OptionPrice;
    }
    protected void setOptionPrice(final OptionPrice value) {
       OptionPrice = value;
    }

   /**
    *1 symbol for Alte Tax number
    */
    public String AlteTaxNum;
    public String getAlteTaxNum() {
       return AlteTaxNum;
    }
    protected void setAlteTaxNum(final String value) {
       AlteTaxNum = value;
    }

   /**
    *Up to 11 symbols for Alte tax value
    */
    public Double AlteTaxValue;
    public Double getAlteTaxValue() {
       return AlteTaxValue;
    }
    protected void setAlteTaxValue(final Double value) {
       AlteTaxValue = value;
    }
}
