package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class DepartmentRes {
   /**
    *1..2 symbols for department number in format ##
    */
    public Double DepNum;
    public Double getDepNum() {
       return DepNum;
    }
    protected void setDepNum(Double value) {
       DepNum = value;
    }

   /**
    *34 symbols for department name
    */
    public String DepName;
    public String getDepName() {
       return DepName;
    }
    protected void setDepName(String value) {
       DepName = value;
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
    protected void setOptionVATClass(OptionVATClass value) {
       OptionVATClass = value;
    }

   /**
    *1..11 symbols for accumulated turnover of the department
    */
    public Double Turnover;
    public Double getTurnover() {
       return Turnover;
    }
    protected void setTurnover(Double value) {
       Turnover = value;
    }

   /**
    *1..11 symbols for sold quantity of the department
    */
    public Double SoldQuantity;
    public Double getSoldQuantity() {
       return SoldQuantity;
    }
    protected void setSoldQuantity(Double value) {
       SoldQuantity = value;
    }

   /**
    *1..5 symbols for the number of last Z report in format #####
    */
    public Double LastZReportNumber;
    public Double getLastZReportNumber() {
       return LastZReportNumber;
    }
    protected void setLastZReportNumber(Double value) {
       LastZReportNumber = value;
    }

   /**
    *16 symbols for the date and hour in last Z report
    */
    public Date LastZReportDate;
    public Date getLastZReportDate() {
       return LastZReportDate;
    }
    protected void setLastZReportDate(Date value) {
       LastZReportDate = value;
    }

   /**
    *1 to 10 symbols for department price
    */
    public Double Price;
    public Double getPrice() {
       return Price;
    }
    protected void setPrice(Double value) {
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
    protected void setOptionDepPrice(OptionDepPrice value) {
       OptionDepPrice = value;
    }

   /**
    *Up to 7 symbols for PLU Category code in format ####.##
    */
    public Double Category;
    public Double getCategory() {
       return Category;
    }
    protected void setCategory(Double value) {
       Category = value;
    }
}
