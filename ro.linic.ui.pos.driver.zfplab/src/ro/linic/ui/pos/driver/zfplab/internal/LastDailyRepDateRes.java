package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class LastDailyRepDateRes {
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
    *1..4 symbols for the number of the last daily report
    */
    public Double NoLastZDailyReport;
    public Double getNoLastZDailyReport() {
       return NoLastZDailyReport;
    }
    protected void setNoLastZDailyReport(final Double value) {
       NoLastZDailyReport = value;
    }

   /**
    *1..4 symbols for the number of the lastRAM reset
    */
    public Double NoLastRAMReset;
    public Double getNoLastRAMReset() {
       return NoLastRAMReset;
    }
    protected void setNoLastRAMReset(final Double value) {
       NoLastRAMReset = value;
    }
}
