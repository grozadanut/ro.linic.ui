package ro.linic.ui.pos.driver.zfplab.internal;
public class PLU3Res {
   /**
    *5 symbols for article number with leading zeroes in format #####
    */
    public Double PLUNo;
    public Double getPLUNo() {
       return PLUNo;
    }
    protected void setPLUNo(final Double value) {
       PLUNo = value;
    }

   /**
    *13 symbols for article barcode
    */
    public String Barcode;
    public String getBarcode() {
       return Barcode;
    }
    protected void setBarcode(final String value) {
       Barcode = value;
    }
}
