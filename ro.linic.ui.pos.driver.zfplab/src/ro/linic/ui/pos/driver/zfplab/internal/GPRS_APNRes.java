package ro.linic.ui.pos.driver.zfplab.internal;
public class GPRS_APNRes {
   /**
    *(Length) Up to 3 symbols for the APN length
    */
    public Double GPRS_APN_Len;
    public Double getGPRS_APN_Len() {
       return GPRS_APN_Len;
    }
    protected void setGPRS_APN_Len(Double value) {
       GPRS_APN_Len = value;
    }

   /**
    *Up to 100 symbols for the device's GPRS APN
    */
    public String APN;
    public String getAPN() {
       return APN;
    }
    protected void setAPN(String value) {
       APN = value;
    }
}
