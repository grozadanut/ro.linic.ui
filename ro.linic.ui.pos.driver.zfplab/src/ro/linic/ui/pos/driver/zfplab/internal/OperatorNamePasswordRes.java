package ro.linic.ui.pos.driver.zfplab.internal;
public class OperatorNamePasswordRes {
   /**
    *Symbol from 1 to 20 corresponding to the number of operator
    */
    public Double Number;
    public Double getNumber() {
       return Number;
    }
    protected void setNumber(final Double value) {
       Number = value;
    }

   /**
    *20 symbols for operator's name
    */
    public String Name;
    public String getName() {
       return Name;
    }
    protected void setName(final String value) {
       Name = value;
    }

   /**
    *4 symbols for operator's password
    */
    public String Password;
    public String getPassword() {
       return Password;
    }
    protected void setPassword(final String value) {
       Password = value;
    }
}
