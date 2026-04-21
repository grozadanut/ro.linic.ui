package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class LastDailyReportInfoRes {
   /**
    *10 symbols for last Z-report date in DD-MM-YYYY format
    */
    public Date LastZDailyReportDate;
    public Date getLastZDailyReportDate() {
       return LastZDailyReportDate;
    }
    protected void setLastZDailyReportDate(final Date value) {
       LastZDailyReportDate = value;
    }

   /**
    *Up to 4 symbols for the number of the last daily report
    */
    public Double LastZDailyReportNum;
    public Double getLastZDailyReportNum() {
       return LastZDailyReportNum;
    }
    protected void setLastZDailyReportNum(final Double value) {
       LastZDailyReportNum = value;
    }

   /**
    *Up to 4 symbols for the number of the last RAM reset
    */
    public Double LastRAMResetNum;
    public Double getLastRAMResetNum() {
       return LastRAMResetNum;
    }
    protected void setLastRAMResetNum(final Double value) {
       LastRAMResetNum = value;
    }
}
