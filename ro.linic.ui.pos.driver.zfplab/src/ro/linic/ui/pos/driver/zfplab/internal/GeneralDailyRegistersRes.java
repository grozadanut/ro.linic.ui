package ro.linic.ui.pos.driver.zfplab.internal;
public class GeneralDailyRegistersRes {
   /**
    *Up to 5 symbols for number of customers
    */
    public Double CustomersNum;
    public Double getCustomersNum() {
       return CustomersNum;
    }
    protected void setCustomersNum(final Double value) {
       CustomersNum = value;
    }

   /**
    *Up to 5 symbols for number of discounts
    */
    public Double DiscountsNum;
    public Double getDiscountsNum() {
       return DiscountsNum;
    }
    protected void setDiscountsNum(final Double value) {
       DiscountsNum = value;
    }

   /**
    *Up to 11 symbols for accumulated amount of discounts
    */
    public Double DiscountsAmount;
    public Double getDiscountsAmount() {
       return DiscountsAmount;
    }
    protected void setDiscountsAmount(final Double value) {
       DiscountsAmount = value;
    }

   /**
    *Up to 5 symbols for number of additions
    */
    public Double AdditionsNum;
    public Double getAdditionsNum() {
       return AdditionsNum;
    }
    protected void setAdditionsNum(final Double value) {
       AdditionsNum = value;
    }

   /**
    *Up to 11 symbols for accumulated amount of additions
    */
    public Double AdditionsAmount;
    public Double getAdditionsAmount() {
       return AdditionsAmount;
    }
    protected void setAdditionsAmount(final Double value) {
       AdditionsAmount = value;
    }

   /**
    *Up to 5 symbols for number of corrections
    */
    public Double CorrectionsNum;
    public Double getCorrectionsNum() {
       return CorrectionsNum;
    }
    protected void setCorrectionsNum(final Double value) {
       CorrectionsNum = value;
    }

   /**
    *Up to 11 symbols for accumulated amount of corrections
    */
    public Double CorrectionsAmount;
    public Double getCorrectionsAmount() {
       return CorrectionsAmount;
    }
    protected void setCorrectionsAmount(final Double value) {
       CorrectionsAmount = value;
    }
}
