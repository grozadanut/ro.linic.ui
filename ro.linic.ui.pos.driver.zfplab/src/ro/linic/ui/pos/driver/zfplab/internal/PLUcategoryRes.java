package ro.linic.ui.pos.driver.zfplab.internal;
public class PLUcategoryRes {
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
