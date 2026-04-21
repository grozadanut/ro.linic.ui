package ro.linic.ui.pos.driver.zfplab.internal;
public class CustomerDataRes {
   /**
    *3 symbols for customer number in order in format ###
    */
    public Double CustomerNum;
    public Double getCustomerNum() {
       return CustomerNum;
    }
    protected void setCustomerNum(final Double value) {
       CustomerNum = value;
    }

   /**
    *15 symbols for customer VAT registration number
    */
    public String CustomerVatNum;
    public String getCustomerVatNum() {
       return CustomerVatNum;
    }
    protected void setCustomerVatNum(final String value) {
       CustomerVatNum = value;
    }

   /**
    *30 symbols for customer name
    */
    public String CustomerName;
    public String getCustomerName() {
       return CustomerName;
    }
    protected void setCustomerName(final String value) {
       CustomerName = value;
    }

   /**
    *30 symbols for customer address
    */
    public String CustomerAddress;
    public String getCustomerAddress() {
       return CustomerAddress;
    }
    protected void setCustomerAddress(final String value) {
       CustomerAddress = value;
    }

   /**
    *20 ASCII symbols for customer data
    */
    public String FreeLine1;
    public String getFreeLine1() {
       return FreeLine1;
    }
    protected void setFreeLine1(final String value) {
       FreeLine1 = value;
    }

   /**
    *20 ASCII symbols for customer data
    */
    public String FreeLine2;
    public String getFreeLine2() {
       return FreeLine2;
    }
    protected void setFreeLine2(final String value) {
       FreeLine2 = value;
    }

   /**
    *20 ASCII symbols for customer data
    */
    public String FreeLine3;
    public String getFreeLine3() {
       return FreeLine3;
    }
    protected void setFreeLine3(final String value) {
       FreeLine3 = value;
    }

   /**
    *20 ASCII symbols for customer data
    */
    public String FreeLine4;
    public String getFreeLine4() {
       return FreeLine4;
    }
    protected void setFreeLine4(final String value) {
       FreeLine4 = value;
    }

   /**
    *1..11 symbols for accumulated turnover of the customer
    */
    public Double CustTurnover;
    public Double getCustTurnover() {
       return CustTurnover;
    }
    protected void setCustTurnover(final Double value) {
       CustTurnover = value;
    }
}
