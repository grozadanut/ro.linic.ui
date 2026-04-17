package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class DailyCountersRes {
   /**
    *Up to 5 symbols for number of the last report from reset
    */
    public Double NoRep;
    public Double getNoRep() {
       return NoRep;
    }
    protected void setNoRep(final Double value) {
       NoRep = value;
    }

   /**
    *Up to 5 symbols for number of the last FM report
    */
    public Double NoLastFMBlock;
    public Double getNoLastFMBlock() {
       return NoLastFMBlock;
    }
    protected void setNoLastFMBlock(final Double value) {
       NoLastFMBlock = value;
    }

   /**
    *Up to 5 symbols for number of EJ
    */
    public Double NoEJ;
    public Double getNoEJ() {
       return NoEJ;
    }
    protected void setNoEJ(final Double value) {
       NoEJ = value;
    }

   /**
    *16 symbols for date and time of the last block storage in FM
    */
    public Date DateTime;
    public Date getDateTime() {
       return DateTime;
    }
    protected void setDateTime(final Date value) {
       DateTime = value;
    }
}
