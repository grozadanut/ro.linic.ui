package ro.linic.ui.pos.driver.zfplab.internal;
public class LastAndTotalReceiptNumRes {
   /**
    *4 symbols for the number of last issued fiscal receipt in format ####
    */
    public Double LastReceiptNum;
    public Double getLastReceiptNum() {
       return LastReceiptNum;
    }
    protected void setLastReceiptNum(final Double value) {
       LastReceiptNum = value;
    }

   /**
    *7 symbols for the number of totals issued fiscal receipts in format #######
    */
    public Double TotalReceiptCounter;
    public Double getTotalReceiptCounter() {
       return TotalReceiptCounter;
    }
    protected void setTotalReceiptCounter(final Double value) {
       TotalReceiptCounter = value;
    }
}
