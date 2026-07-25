package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class LastANAFReportInfoRes {
   /**
    *4 symbols representing the number of the last report sent to the ANAF 
    *server
    */
    public String ReportNumber;
    public String getReportNumber() {
       return ReportNumber;
    }
    protected void setReportNumber(String value) {
       ReportNumber = value;
    }

   /**
    *Date Time parameter in format: DD-MM-YY [Space] hh:mm:ss
    */
    public Date DateTime;
    public Date getDateTime() {
       return DateTime;
    }
    protected void setDateTime(Date value) {
       DateTime = value;
    }
}
