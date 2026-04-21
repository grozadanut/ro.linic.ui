package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class PLUallDataRes {
   /**
    *5 symbols for article number with leading zeroes in format: #####
    */
    public Double PLUNum;
    public Double getPLUNum() {
       return PLUNum;
    }
    protected void setPLUNum(final Double value) {
       PLUNum = value;
    }

   /**
    *34 symbols for article name (LF=7Ch, MU separator = 80h or 60h 
    *followed up to 3 symbols for unit)
    */
    public String PLUName;
    public String getPLUName() {
       return PLUName;
    }
    protected void setPLUName(final String value) {
       PLUName = value;
    }

   /**
    *1..10 symbols for article price
    */
    public Double Price;
    public Double getPrice() {
       return Price;
    }
    protected void setPrice(final Double value) {
       Price = value;
    }

   /**
    *1 symbol for flags = 0x80 + FlagSinglTr + FlagQTY + OptionPrice 
    *Where  
    *OptionPrice: 
    *0x00 - for free price is disable /valid only programmed price/ 
    *0x01 - for free price is enable 
    *0x02 - for limited price 
    *FlagQTY: 
    *0x00 - for availability of PLU stock is not monitored 
    *0x04 - for disable negative quantity 
    *0x08 - for enable negative quantity 
    *FlagSingleTr: 
    *0x00 - no single transaction 
    *0x10 - single transaction is active
    */
    public byte FlagsPricePLU;
    public byte getFlagsPricePLU() {
       return FlagsPricePLU;
    }
    protected void setFlagsPricePLU(final byte value) {
       FlagsPricePLU = value;
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
    public Double Turnover;
    public Double getTurnover() {
       return Turnover;
    }
    protected void setTurnover(final Double value) {
       Turnover = value;
    }

   /**
    *Up to 11 symbols for Sales quantity of the article
    */
    public Double QuantitySold;
    public Double getQuantitySold() {
       return QuantitySold;
    }
    protected void setQuantitySold(final Double value) {
       QuantitySold = value;
    }

   /**
    *Up to 5 symbols for the number of the last article report with zeroing
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
    *(Available Quantity) - 1..11 symbols for quantity in stock
    */
    public Double AvailableQTY;
    public Double getAvailableQTY() {
       return AvailableQTY;
    }
    protected void setAvailableQTY(final Double value) {
       AvailableQTY = value;
    }

   /**
    *13 symbols for article barcode
    */
    public String Barcode;
    public String getBarcode() {
       return Barcode;
    }
    protected void setBarcode(final String value) {
       Barcode = value;
    }

   /**
    *Up to 11 symbols for Alte tax amount
    */
    public Double AlteTaxAmount;
    public Double getAlteTaxAmount() {
       return AlteTaxAmount;
    }
    protected void setAlteTaxAmount(final Double value) {
       AlteTaxAmount = value;
    }

   /**
    *Up to 7 symbols for PLU Category code in format ####.##
    */
    public Double Category;
    public Double getCategory() {
       return Category;
    }
    protected void setCategory(final Double value) {
       Category = value;
    }
}
