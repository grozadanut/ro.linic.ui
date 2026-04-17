package ro.linic.ui.pos.driver.zfplab.internal;
public class LastReceiptNoRes {
   /**
    *1..4 symbols for the number of the last issued receipt by FPR in format ####
    */
    public Double NoLastIsRcp;
    public Double getNoLastIsRcp() {
       return NoLastIsRcp;
    }
    protected void setNoLastIsRcp(final Double value) {
       NoLastIsRcp = value;
    }

   /**
    *1..7 symbols for the number of the total issued receipts by FPR 
    *in format #######
    */
    public Double NoTotalRcp;
    public Double getNoTotalRcp() {
       return NoTotalRcp;
    }
    protected void setNoTotalRcp(final Double value) {
       NoTotalRcp = value;
    }
}
