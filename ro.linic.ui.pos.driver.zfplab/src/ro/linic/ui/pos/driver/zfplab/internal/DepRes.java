package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class DepRes {
   /**
    *2 symbols for department number in format ##
    */
    public Double Number;
    public Double getNumber() {
       return Number;
    }
    protected void setNumber(final Double value) {
       Number = value;
    }

   /**
    *34 symbols for department name
    */
    public String Name;
    public String getName() {
       return Name;
    }
    protected void setName(final String value) {
       Name = value;
    }

   /**
    *1 symbol for article's VAT class with optional values:" 
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
    *1..11 symbols for accumulated turnover of the department
    */
    public Double Turnover;
    public Double getTurnover() {
       return Turnover;
    }
    protected void setTurnover(final Double value) {
       Turnover = value;
    }

   /**
    *1..11 symbols for sold quantity of the department
    */
    public Double QtySold;
    public Double getQtySold() {
       return QtySold;
    }
    protected void setQtySold(final Double value) {
       QtySold = value;
    }

   /**
    *5 symbols for the number of last Z report in format #####
    */
    public Double LastZrepNum;
    public Double getLastZrepNum() {
       return LastZrepNum;
    }
    protected void setLastZrepNum(final Double value) {
       LastZrepNum = value;
    }

   /**
    *16 symbols for the date and hour in last Z report
    */
    public Date LastZrepDate;
    public Date getLastZrepDate() {
       return LastZrepDate;
    }
    protected void setLastZrepDate(final Date value) {
       LastZrepDate = value;
    }

   /**
    *1 to 10 symbols for department price
    */
    public Double Price;
    public Double getPrice() {
       return Price;
    }
    protected void setPrice(final Double value) {
       Price = value;
    }

   /**
    *1 symbol for Department flags with next value: 
    * - '0' - Free price disabled  
    * - '1' - Free price enabled  
    * - '2' - Limited price 
    * - '4' - Free price disabled for single transaction 
    * - '5' - Free price enabled for single transaction 
    * - '6' - Limited price for single transaction
    */
    public OptionDepPrice OptionDepPrice;
    public OptionDepPrice getOptionDepPrice() {
       return OptionDepPrice;
    }
    protected void setOptionDepPrice(final OptionDepPrice value) {
       OptionDepPrice = value;
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
