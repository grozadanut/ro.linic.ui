package ro.linic.ui.pos.driver.zfplab.internal;
public class GPRS_UsernameRes {
   /**
    *(Length) Up to 3 symbols for the GPRS username length
    */
    public Double GPRS_User_Len;
    public Double getGPRS_User_Len() {
       return GPRS_User_Len;
    }
    protected void setGPRS_User_Len(Double value) {
       GPRS_User_Len = value;
    }

   /**
    *Up to 100 symbols for the device's GPRS username
    */
    public String Username;
    public String getUsername() {
       return Username;
    }
    protected void setUsername(String value) {
       Username = value;
    }
}
