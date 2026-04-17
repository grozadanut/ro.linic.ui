package ro.linic.ui.pos.driver.zfplab.internal;
public class FooterRes {
   /**
    *(Line Number)1 symbol with value: 
    * - '1' - Footer 1 
    * - '2' - Footer 2 
    * - '3' - Footer 3
    */
    public OptionFooterLine OptionFooterLine;
    public OptionFooterLine getOptionFooterLine() {
       return OptionFooterLine;
    }
    protected void setOptionFooterLine(final OptionFooterLine value) {
       OptionFooterLine = value;
    }

   /**
    *LineLength symbols for footer line
    */
    public String FooterText;
    public String getFooterText() {
       return FooterText;
    }
    protected void setFooterText(final String value) {
       FooterText = value;
    }
}
