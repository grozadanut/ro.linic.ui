package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class PLU0Res {
   /**
    *1..5 symbols for article number with leading zeroes in format: #####
    */
    public Double PLUNo;
    public Double getPLUNo() {
       return PLUNo;
    }
    protected void setPLUNo(final Double value) {
       PLUNo = value;
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
    *
    *
    * - 'A' - VAT class A 
    * - 'B' - VAT class B 
    * - 'C' - VAT class C 
    * - 'D' - VAT class D 
    * - 'E' - VAT class E 
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
    public int BelongToDepNo;
    public int getBelongToDepNo() {
       return BelongToDepNo;
    }
    protected void setBelongToDepNo(final int value) {
       BelongToDepNo = value;
    }

   /**
    *1..11 symbol for Alte Tax number
    */
    public Double AlteTaxNum;
    public Double getAlteTaxNum() {
       return AlteTaxNum;
    }
    protected void setAlteTaxNum(final Double value) {
       AlteTaxNum = value;
    }

   /**
    *1..11 symbols for Alte tax value
    */
    public Double AlteTaxValue;
    public Double getAlteTaxValue() {
       return AlteTaxValue;
    }
    protected void setAlteTaxValue(final Double value) {
       AlteTaxValue = value;
    }

   /**
    *1..11 symbols for PLU accumulated turnover
    */
    public Double TurnoverAmount;
    public Double getTurnoverAmount() {
       return TurnoverAmount;
    }
    protected void setTurnoverAmount(final Double value) {
       TurnoverAmount = value;
    }

   /**
    *1..11 symbols for Sales quantity of the article
    */
    public Double SoldQuantity;
    public Double getSoldQuantity() {
       return SoldQuantity;
    }
    protected void setSoldQuantity(final Double value) {
       SoldQuantity = value;
    }

   /**
    *1..5 symbols for the number of the last article report with zeroing
    */
    public Double LastZrepNum;
    public Double getLastZrepNum() {
       return LastZrepNum;
    }
    protected void setLastZrepNum(final Double value) {
       LastZrepNum = value;
    }

   /**
    *16 symbols for the date and time of the last article report with zeroing
    */
    public Date LastZreportDate;
    public Date getLastZreportDate() {
       return LastZreportDate;
    }
    protected void setLastZreportDate(final Date value) {
       LastZreportDate = value;
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
    *1..11 symbols for Alte tax amount
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
