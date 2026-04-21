package ro.linic.ui.pos.driver.zfplab.internal;
public class DailyCurrencyRes {
   /**
    *Up to 12 symbols for accumulated amount of sales of currency without 
    *commission
    */
    public Double Vanzari;
    public Double getVanzari() {
       return Vanzari;
    }
    protected void setVanzari(final Double value) {
       Vanzari = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of currency purchase 
    *without commission
    */
    public Double Cumparari;
    public Double getCumparari() {
       return Cumparari;
    }
    protected void setCumparari(final Double value) {
       Cumparari = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of commissions in sales
    */
    public Double Commission_vanzari;
    public Double getCommission_vanzari() {
       return Commission_vanzari;
    }
    protected void setCommission_vanzari(final Double value) {
       Commission_vanzari = value;
    }

   /**
    *Up to 12 symbols for accumulated amount of commissions in 
    *purchase
    */
    public Double Commission_cumparari;
    public Double getCommission_cumparari() {
       return Commission_cumparari;
    }
    protected void setCommission_cumparari(final Double value) {
       Commission_cumparari = value;
    }
}
