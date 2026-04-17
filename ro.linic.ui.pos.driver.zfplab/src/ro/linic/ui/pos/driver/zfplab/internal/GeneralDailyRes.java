package ro.linic.ui.pos.driver.zfplab.internal;
public class GeneralDailyRes {
   /**
    *1..5 symbols for number of customers
    */
    public Double NoCust;
    public Double getNoCust() {
       return NoCust;
    }
    protected void setNoCust(final Double value) {
       NoCust = value;
    }

   /**
    *1..5 symbols for number of discounts
    */
    public Double NoDisc;
    public Double getNoDisc() {
       return NoDisc;
    }
    protected void setNoDisc(final Double value) {
       NoDisc = value;
    }

   /**
    *1..11 symbols for accumulated amount of discounts
    */
    public Double AmntDisc;
    public Double getAmntDisc() {
       return AmntDisc;
    }
    protected void setAmntDisc(final Double value) {
       AmntDisc = value;
    }

   /**
    *1..5 symbols for number of additions
    */
    public Double NoAdd;
    public Double getNoAdd() {
       return NoAdd;
    }
    protected void setNoAdd(final Double value) {
       NoAdd = value;
    }

   /**
    *1..11 symbols for accumulated amount of additions
    */
    public Double AmntAdd;
    public Double getAmntAdd() {
       return AmntAdd;
    }
    protected void setAmntAdd(final Double value) {
       AmntAdd = value;
    }

   /**
    *1..5 symbols for number of corrections
    */
    public Double NoVoid;
    public Double getNoVoid() {
       return NoVoid;
    }
    protected void setNoVoid(final Double value) {
       NoVoid = value;
    }

   /**
    *1..11 symbols for accumulated amount of corrections
    */
    public Double AmntVoid;
    public Double getAmntVoid() {
       return AmntVoid;
    }
    protected void setAmntVoid(final Double value) {
       AmntVoid = value;
    }
}
