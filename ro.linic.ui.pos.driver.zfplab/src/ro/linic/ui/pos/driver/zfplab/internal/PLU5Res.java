package ro.linic.ui.pos.driver.zfplab.internal;
public class PLU5Res {
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
    *Up to 7 symbols for PLU Category code in format ####.##
    */
    public Double Category;
    public Double getCategory() {
       return Category;
    }
    protected void setCategory(final Double value) {
       Category = value;
    }
}
