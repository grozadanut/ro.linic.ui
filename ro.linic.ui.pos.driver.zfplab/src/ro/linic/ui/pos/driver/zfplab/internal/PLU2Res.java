package ro.linic.ui.pos.driver.zfplab.internal;
public class PLU2Res {
   /**
    *5 symbols for article number with leading zeroes in format #####
    */
    public Double PLUNo;
    public Double getPLUNo() {
       return PLUNo;
    }
    protected void setPLUNo(Double value) {
       PLUNo = value;
    }

   /**
    *(Available Quantity) - 1..11 symbols for quantity in stock
    */
    public Double AvailQTY;
    public Double getAvailQTY() {
       return AvailQTY;
    }
    protected void setAvailQTY(Double value) {
       AvailQTY = value;
    }

   /**
    *1 byte for Quantity option with next value: 
    * - '0'- Availability of PLU stock is not monitored 
    * - '1'- Disable Negative Quantity 
    * - '2'- Enable Negative Quantity
    */
    public OptionQTY OptionQTY;
    public OptionQTY getOptionQTY() {
       return OptionQTY;
    }
    protected void setOptionQTY(OptionQTY value) {
       OptionQTY = value;
    }
}
