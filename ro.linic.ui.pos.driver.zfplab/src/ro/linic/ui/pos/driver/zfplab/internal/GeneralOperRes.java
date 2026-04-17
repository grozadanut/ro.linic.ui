package ro.linic.ui.pos.driver.zfplab.internal;
public class GeneralOperRes {
   /**
    *Symbol from 1 to 20corresponding to operator's number
    */
    public Double OpNo;
    public Double getOpNo() {
       return OpNo;
    }
    protected void setOpNo(final Double value) {
       OpNo = value;
    }

   /**
    *Up to 5 symbols for number of customers
    */
    public Double NoCustomers;
    public Double getNoCustomers() {
       return NoCustomers;
    }
    protected void setNoCustomers(final Double value) {
       NoCustomers = value;
    }

   /**
    *Up to 5 symbols for number of discounts
    */
    public Double NoDiscounts;
    public Double getNoDiscounts() {
       return NoDiscounts;
    }
    protected void setNoDiscounts(final Double value) {
       NoDiscounts = value;
    }

   /**
    *Up to 11 symbols for accumulated amount of discounts
    */
    public Double AmountDiscounts;
    public Double getAmountDiscounts() {
       return AmountDiscounts;
    }
    protected void setAmountDiscounts(final Double value) {
       AmountDiscounts = value;
    }

   /**
    *Up to 5 symbols for number ofadditions
    */
    public Double NoAdditions;
    public Double getNoAdditions() {
       return NoAdditions;
    }
    protected void setNoAdditions(final Double value) {
       NoAdditions = value;
    }

   /**
    *Up to 11 symbols for accumulated amount ofadditions
    */
    public Double AmountAdditions;
    public Double getAmountAdditions() {
       return AmountAdditions;
    }
    protected void setAmountAdditions(final Double value) {
       AmountAdditions = value;
    }

   /**
    *Up to 5 symbols for number of corrections
    */
    public Double NoVoids;
    public Double getNoVoids() {
       return NoVoids;
    }
    protected void setNoVoids(final Double value) {
       NoVoids = value;
    }

   /**
    *Up to 11 symbols for accumulated amount of corrections
    */
    public Double AmountVoids;
    public Double getAmountVoids() {
       return AmountVoids;
    }
    protected void setAmountVoids(final Double value) {
       AmountVoids = value;
    }
}
