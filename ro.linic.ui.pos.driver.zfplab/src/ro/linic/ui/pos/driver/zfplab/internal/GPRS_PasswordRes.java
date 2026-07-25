package ro.linic.ui.pos.driver.zfplab.internal;
public class GPRS_PasswordRes {
   /**
    *(Length) Up to 3 symbols for the GPRS password length
    */
    public Double PassLength;
    public Double getPassLength() {
       return PassLength;
    }
    protected void setPassLength(Double value) {
       PassLength = value;
    }

   /**
    *Up to 100 symbols for the device's GPRS password
    */
    public String Password;
    public String getPassword() {
       return Password;
    }
    protected void setPassword(String value) {
       Password = value;
    }
}
