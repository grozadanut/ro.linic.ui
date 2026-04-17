package ro.linic.ui.pos.driver.zfplab.internal;
public class AlteTaxeRes {
   /**
    *1 symbol for number in order
    */
    public String Number;
    public String getNumber() {
       return Number;
    }
    protected void setNumber(final String value) {
       Number = value;
    }

   /**
    *12 symbols for Alte taxe name
    */
    public String Name;
    public String getName() {
       return Name;
    }
    protected void setName(final String value) {
       Name = value;
    }
}
