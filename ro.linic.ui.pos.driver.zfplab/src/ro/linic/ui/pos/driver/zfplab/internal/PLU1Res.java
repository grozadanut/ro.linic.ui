package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class PLU1Res {
   /**
    *5 symbols for article number with leading zeroes in format #####
    */
    public Double PLUNo;
    public Double getPLUNo() {
       return PLUNo;
    }
    protected void setPLUNo(Double value) {
       PLUNo = value;
    }

   /**
    *34 symbols for article name /LF=7Ch, MU separator = 80h or 60h followed up 
    *to 3 symbols for unit/
    */
    public String PLUName;
    public String getPLUName() {
       return PLUName;
    }
    protected void setPLUName(String value) {
       PLUName = value;
    }

   /**
    *1..10 symbols for article price
    */
    public Double Price;
    public Double getPrice() {
       return Price;
    }
    protected void setPrice(Double value) {
       Price = value;
    }

   /**
    *1 symbol for price flag with next value: 
    * - '0'- Free price is disable /valid only programmed price/ 
    * - '1'- Free price is enable 
    * - '2'- Limited price
    */
    public OptionPrice OptionPrice;
    public OptionPrice getOptionPrice() {
       return OptionPrice;
    }
    protected void setOptionPrice(OptionPrice value) {
       OptionPrice = value;
    }

   /**
    *1 symbol for article's VAT class with optional values: 
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
    protected void setOptionVATClass(OptionVATClass value) {
       OptionVATClass = value;
    }

   /**
    *BelongToDepNo + 80h, 1 symbol for PLU department = 0x80 … 0x93
    */
    public int BelongToDepNo;
    public int getBelongToDepNo() {
       return BelongToDepNo;
    }
    protected void setBelongToDepNo(int value) {
       BelongToDepNo = value;
    }

   /**
    *1..11 symbol for Alte Tax number
    */
    public Double AlteTaxNum;
    public Double getAlteTaxNum() {
       return AlteTaxNum;
    }
    protected void setAlteTaxNum(Double value) {
       AlteTaxNum = value;
    }

   /**
    *1..11 symbols for Alte tax value
    */
    public Double AlteTaxValue;
    public Double getAlteTaxValue() {
       return AlteTaxValue;
    }
    protected void setAlteTaxValue(Double value) {
       AlteTaxValue = value;
    }

   /**
    *1..11 symbols for PLU accumulated turnover
    */
    public Double TurnoverAmount;
    public Double getTurnoverAmount() {
       return TurnoverAmount;
    }
    protected void setTurnoverAmount(Double value) {
       TurnoverAmount = value;
    }

   /**
    *1..11 symbols for Sales quantity of the article
    */
    public Double SoldQuantity;
    public Double getSoldQuantity() {
       return SoldQuantity;
    }
    protected void setSoldQuantity(Double value) {
       SoldQuantity = value;
    }

   /**
    *5 symbols for the number of the last article report with zeroing in format #####
    */
    public Double NoLastZ;
    public Double getNoLastZ() {
       return NoLastZ;
    }
    protected void setNoLastZ(Double value) {
       NoLastZ = value;
    }

   /**
    *16 symbols for the date and time of the last article report with zeroing
    */
    public Date LastZreportDate;
    public Date getLastZreportDate() {
       return LastZreportDate;
    }
    protected void setLastZreportDate(Date value) {
       LastZreportDate = value;
    }

   /**
    *1 symbol with value: 
    * - '1' - Active Single transaction in receipt  
    * - '0' - Inactive /default value/
    */
    public OptionSingleTr OptionSingleTr;
    public OptionSingleTr getOptionSingleTr() {
       return OptionSingleTr;
    }
    protected void setOptionSingleTr(OptionSingleTr value) {
       OptionSingleTr = value;
    }

   /**
    *1.11 symbols for Alte Tax Turnover
    */
    public Double AlteTaxTurnover;
    public Double getAlteTaxTurnover() {
       return AlteTaxTurnover;
    }
    protected void setAlteTaxTurnover(Double value) {
       AlteTaxTurnover = value;
    }
}
