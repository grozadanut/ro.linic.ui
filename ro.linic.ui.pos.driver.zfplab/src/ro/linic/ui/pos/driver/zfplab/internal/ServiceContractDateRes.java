package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class ServiceContractDateRes {
   /**
    *6 symbols for service password 
    *'1' '1' - for date
    */
    public String Password;
    public String getPassword() {
       return Password;
    }
    protected void setPassword(final String value) {
       Password = value;
    }

   /**
    *10 symbols for expiry date of service contract
    */
    public Date ExpiryDate;
    public Date getExpiryDate() {
       return ExpiryDate;
    }
    protected void setExpiryDate(final Date value) {
       ExpiryDate = value;
    }
}
