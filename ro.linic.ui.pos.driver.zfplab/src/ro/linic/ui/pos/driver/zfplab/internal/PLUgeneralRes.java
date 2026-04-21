package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class PLUgeneralRes {
   /**
    *5 symbols for article number with leading zeroes in format #####
    */
    public Double PLUNum;
    public Double getPLUNum() {
       return PLUNum;
    }
    protected void setPLUNum(final Double value) {
       PLUNum = value;
    }

   /**
    *34 symbols for article name /LF=7Ch, MU separator = 80h or 60h 
    *followed up to 3 symbols for unit/
    */
    public String PLUName;
    public String getPLUName() {
       return PLUName;
    }
    protected void setPLUName(final String value) {
       PLUName = value;
    }

   /**
    *Up to 10 symbols for article price
    */
    public Double Price;
    public Double getPrice() {
       return Price;
    }
    protected void setPrice(final Double value) {
       Price = value;
    }

   /**
    *1 symbol for price flag with next value: 
    * - '0'- Free price is disable valid only programmed price 
    * - '1'- Free price is enable 
    * - '2'- Limited price
    */
    public OptionPrice OptionPrice;
    public OptionPrice getOptionPrice() {
       return OptionPrice;
    }
    protected void setOptionPrice(final OptionPrice value) {
       OptionPrice = value;
    }

   /**
    *1 symbol for article's VAT class with optional values:" 
    * - 'A' - VAT Class A 
    * - 'B' - VAT Class B 
    * - 'C' - VAT Class C 
    * - 'D' - VAT Class D 
    * - 'E' - VAT Class E 
    * - 'F' - Alte taxe
    */
    public OptionVATClass OptionVATClass;
    public OptionVATClass getOptionVATClass() {
       return OptionVATClass;
    }
    protected void setOptionVATClass(final OptionVATClass value) {
       OptionVATClass = value;
    }

   /**
    *BelongToDepNo + 80h, 1 symbol for PLU department = 0x80 … 0x93
    */
    public int BelongToDepNumber;
    public int getBelongToDepNumber() {
       return BelongToDepNumber;
    }
    protected void setBelongToDepNumber(final int value) {
       BelongToDepNumber = value;
    }

   /**
    *Up to 11 symbols for Alte Tax number
    */
    public Double AlteTaxNum;
    public Double getAlteTaxNum() {
       return AlteTaxNum;
    }
    protected void setAlteTaxNum(final Double value) {
       AlteTaxNum = value;
    }

   /**
    *Up to 11 symbols for Alte tax value
    */
    public Double AlteTaxValue;
    public Double getAlteTaxValue() {
       return AlteTaxValue;
    }
    protected void setAlteTaxValue(final Double value) {
       AlteTaxValue = value;
    }

   /**
    *Up to 11 symbols for PLU accumulated turnover
    */
    public Double TurnoverAmount;
    public Double getTurnoverAmount() {
       return TurnoverAmount;
    }
    protected void setTurnoverAmount(final Double value) {
       TurnoverAmount = value;
    }

   /**
    *Up to 11 symbols for Sales quantity of the article
    */
    public Double SoldQuantity;
    public Double getSoldQuantity() {
       return SoldQuantity;
    }
    protected void setSoldQuantity(final Double value) {
       SoldQuantity = value;
    }

   /**
    *5 symbols for the number of the last in format #####  
    *article report with zeroing
    */
    public Double LastZReportNumber;
    public Double getLastZReportNumber() {
       return LastZReportNumber;
    }
    protected void setLastZReportNumber(final Double value) {
       LastZReportNumber = value;
    }

   /**
    *16 symbols for the date and time of the last article report with zeroing
    */
    public Date LastZReportDate;
    public Date getLastZReportDate() {
       return LastZReportDate;
    }
    protected void setLastZReportDate(final Date value) {
       LastZReportDate = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - Inactive, default value 
    * - '1' - Active Single transaction in receipt
    */
    public OptionSingleTransaction OptionSingleTransaction;
    public OptionSingleTransaction getOptionSingleTransaction() {
       return OptionSingleTransaction;
    }
    protected void setOptionSingleTransaction(final OptionSingleTransaction value) {
       OptionSingleTransaction = value;
    }

   /**
    *Up to 11 symbols for Alte Tax Turnover
    */
    public Double AlteTaxTurnover;
    public Double getAlteTaxTurnover() {
       return AlteTaxTurnover;
    }
    protected void setAlteTaxTurnover(final Double value) {
       AlteTaxTurnover = value;
    }
}
