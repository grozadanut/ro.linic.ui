package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class DailyCountersRes {
   /**
    *Up to 5 symbols for number of the last report from reset
    */
    public Double LastReportNumFromReset;
    public Double getLastReportNumFromReset() {
       return LastReportNumFromReset;
    }
    protected void setLastReportNumFromReset(final Double value) {
       LastReportNumFromReset = value;
    }

   /**
    *Up to 5 symbols for number of the last FM report
    */
    public Double NumLastFMBlock;
    public Double getNumLastFMBlock() {
       return NumLastFMBlock;
    }
    protected void setNumLastFMBlock(final Double value) {
       NumLastFMBlock = value;
    }

   /**
    *Up to 5 symbols for number of EJ
    */
    public Double NumEJ;
    public Double getNumEJ() {
       return NumEJ;
    }
    protected void setNumEJ(final Double value) {
       NumEJ = value;
    }

   /**
    *16 symbols for date and time of the last block storage in FM in 
    *format "DD-MM-YYYY HH:MM"
    */
    public Date DateTime;
    public Date getDateTime() {
       return DateTime;
    }
    protected void setDateTime(final Date value) {
       DateTime = value;
    }
}
