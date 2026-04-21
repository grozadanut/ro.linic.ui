package ro.linic.ui.pos.driver.zfplab.internal;
public class CurrencyAmountsByOperatorRes {
   /**
    *Symbol from 1 to 20 corresponding to operator's number
    */
    public String OperNum;
    public String getOperNum() {
       return OperNum;
    }
    protected void setOperNum(final String value) {
       OperNum = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of sales of currency 
    *without commission
    */
    public Double Vanzari_op;
    public Double getVanzari_op() {
       return Vanzari_op;
    }
    protected void setVanzari_op(final Double value) {
       Vanzari_op = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of currency purchase 
    *without commission
    */
    public Double Cumparari_op;
    public Double getCumparari_op() {
       return Cumparari_op;
    }
    protected void setCumparari_op(final Double value) {
       Cumparari_op = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of commissions in 
    *sales
    */
    public Double Commission_vanzari_op;
    public Double getCommission_vanzari_op() {
       return Commission_vanzari_op;
    }
    protected void setCommission_vanzari_op(final Double value) {
       Commission_vanzari_op = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of commissions in 
    *purchase
    */
    public Double Commission_cumparari_op;
    public Double getCommission_cumparari_op() {
       return Commission_cumparari_op;
    }
    protected void setCommission_cumparari_op(final Double value) {
       Commission_cumparari_op = value;
    }
}
