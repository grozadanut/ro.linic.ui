package ro.linic.ui.pos.driver.zfplab.internal;
public class LastCurrencyReceiptNumRes {
   /**
    *4 symbols in format ####.  
    *For the number of last issued fiscal receipts
    */
    public Double LastReceiptNum;
    public Double getLastReceiptNum() {
       return LastReceiptNum;
    }
    protected void setLastReceiptNum(final Double value) {
       LastReceiptNum = value;
    }

   /**
    *7 symbols in format #######.  
    *For the number of totals issued fiscal receipts
    */
    public Double TotalReceiptCounter;
    public Double getTotalReceiptCounter() {
       return TotalReceiptCounter;
    }
    protected void setTotalReceiptCounter(final Double value) {
       TotalReceiptCounter = value;
    }

   /**
    *4 symbols in format ####.  
    *For the number of sale of currency receipts
    */
    public Double Daily_Vanzari_RecCounter;
    public Double getDaily_Vanzari_RecCounter() {
       return Daily_Vanzari_RecCounter;
    }
    protected void setDaily_Vanzari_RecCounter(final Double value) {
       Daily_Vanzari_RecCounter = value;
    }

   /**
    *4 symbols in format ####. 
    *For the number of currency purchase receipts
    */
    public Double Daily_Cumparari_RecCounter;
    public Double getDaily_Cumparari_RecCounter() {
       return Daily_Cumparari_RecCounter;
    }
    protected void setDaily_Cumparari_RecCounter(final Double value) {
       Daily_Cumparari_RecCounter = value;
    }

   /**
    *4 symbols in format ####. 
    *For the number of cancel receipts
    */
    public Double Daily_Anulat_RecCounter;
    public Double getDaily_Anulat_RecCounter() {
       return Daily_Anulat_RecCounter;
    }
    protected void setDaily_Anulat_RecCounter(final Double value) {
       Daily_Anulat_RecCounter = value;
    }
}
