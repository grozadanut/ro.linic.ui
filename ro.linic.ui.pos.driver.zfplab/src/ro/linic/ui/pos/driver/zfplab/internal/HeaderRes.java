package ro.linic.ui.pos.driver.zfplab.internal;
public class HeaderRes {
   /**
    *(Line Number)1 byte with value: 
    * - '1' - Header 1 
    * - '2' - Header 2 
    * - '3' - Header 3 
    * - '4' - Header 4 
    * - '5' - Header 5 
    * - '6' - Header 6 
    * - '7' - Header 7 
    * - '8' - Header 8
    */
    public OptionHeaderLine OptionHeaderLine;
    public OptionHeaderLine getOptionHeaderLine() {
       return OptionHeaderLine;
    }
    protected void setOptionHeaderLine(final OptionHeaderLine value) {
       OptionHeaderLine = value;
    }

   /**
    *LineLength symbols
    */
    public String HeaderText;
    public String getHeaderText() {
       return HeaderText;
    }
    protected void setHeaderText(final String value) {
       HeaderText = value;
    }
}
