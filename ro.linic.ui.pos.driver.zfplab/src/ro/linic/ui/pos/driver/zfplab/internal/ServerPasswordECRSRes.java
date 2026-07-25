package ro.linic.ui.pos.driver.zfplab.internal;
public class ServerPasswordECRSRes {
   /**
    *Up to 2 symbols for parameter length
    */
    public Double ParamLength;
    public Double getParamLength() {
       return ParamLength;
    }
    protected void setParamLength(Double value) {
       ParamLength = value;
    }

   /**
    *Up to 64 symbols for server password
    */
    public String ServerPassword;
    public String getServerPassword() {
       return ServerPassword;
    }
    protected void setServerPassword(String value) {
       ServerPassword = value;
    }
}
