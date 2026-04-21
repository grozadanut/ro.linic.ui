package ro.linic.ui.pos.driver.zfplab.internal;
public class ServiceWarningMessagesRes {
   /**
    *6 symbols for service password 
    *'3' '3' - for warning message
    */
    public String Password;
    public String getPassword() {
       return Password;
    }
    protected void setPassword(final String value) {
       Password = value;
    }

   /**
    *TextLength symbols for warning message for line 1
    */
    public String Line1;
    public String getLine1() {
       return Line1;
    }
    protected void setLine1(final String value) {
       Line1 = value;
    }

   /**
    *TextLength symbols for warning message for line 2
    */
    public String Line2;
    public String getLine2() {
       return Line2;
    }
    protected void setLine2(final String value) {
       Line2 = value;
    }

   /**
    *TextLength symbols for warning message for line 3
    */
    public String Line3;
    public String getLine3() {
       return Line3;
    }
    protected void setLine3(final String value) {
       Line3 = value;
    }
}
