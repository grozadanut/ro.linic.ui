package ro.linic.ui.pos.driver.zfplab.internal;
public class QRserverAddressRes {
   /**
    *Up to 3 symbols for parameter length
    */
    public Double ParamLength;
    public Double getParamLength() {
       return ParamLength;
    }
    protected void setParamLength(Double value) {
       ParamLength = value;
    }

   /**
    *Up to 100 symbols for server password
    */
    public String ServerAddress;
    public String getServerAddress() {
       return ServerAddress;
    }
    protected void setServerAddress(String value) {
       ServerAddress = value;
    }
}
