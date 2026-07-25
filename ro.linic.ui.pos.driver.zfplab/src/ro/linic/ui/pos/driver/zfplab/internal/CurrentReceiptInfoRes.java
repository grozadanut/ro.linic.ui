package ro.linic.ui.pos.driver.zfplab.internal;
public class CurrentReceiptInfoRes {
   /**
    *1 symbol with value: 
    * - '0' - No 
    * - '1' - Yes
    */
    public OptionIsReceiptOpened OptionIsReceiptOpened;
    public OptionIsReceiptOpened getOptionIsReceiptOpened() {
       return OptionIsReceiptOpened;
    }
    protected void setOptionIsReceiptOpened(OptionIsReceiptOpened value) {
       OptionIsReceiptOpened = value;
    }

   /**
    *3 symbols for number of sales
    */
    public String SalesNumber;
    public String getSalesNumber() {
       return SalesNumber;
    }
    protected void setSalesNumber(String value) {
       SalesNumber = value;
    }

   /**
    *Up to 11 symbols for subtotal by VAT group A
    */
    public Double SubtotalAmountVATGA;
    public Double getSubtotalAmountVATGA() {
       return SubtotalAmountVATGA;
    }
    protected void setSubtotalAmountVATGA(Double value) {
       SubtotalAmountVATGA = value;
    }

   /**
    *Up to 11 symbols for subtotal by VAT group B
    */
    public Double SubtotalAmountVATGB;
    public Double getSubtotalAmountVATGB() {
       return SubtotalAmountVATGB;
    }
    protected void setSubtotalAmountVATGB(Double value) {
       SubtotalAmountVATGB = value;
    }

   /**
    *Up to 11 symbols for subtotal by VAT group C
    */
    public Double SubtotalAmountVATGC;
    public Double getSubtotalAmountVATGC() {
       return SubtotalAmountVATGC;
    }
    protected void setSubtotalAmountVATGC(Double value) {
       SubtotalAmountVATGC = value;
    }

   /**
    *Up to 11 symbols for subtotal by VAT group D
    */
    public Double SubtotalAmountVATGD;
    public Double getSubtotalAmountVATGD() {
       return SubtotalAmountVATGD;
    }
    protected void setSubtotalAmountVATGD(Double value) {
       SubtotalAmountVATGD = value;
    }

   /**
    *Up to 11 symbols for subtotal by VAT group E
    */
    public Double SubtotalAmountVATGE;
    public Double getSubtotalAmountVATGE() {
       return SubtotalAmountVATGE;
    }
    protected void setSubtotalAmountVATGE(Double value) {
       SubtotalAmountVATGE = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - allowed 
    * - '1' - forbidden
    */
    public OptionForbiddenVoid OptionForbiddenVoid;
    public OptionForbiddenVoid getOptionForbiddenVoid() {
       return OptionForbiddenVoid;
    }
    protected void setOptionForbiddenVoid(OptionForbiddenVoid value) {
       OptionForbiddenVoid = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - with printing 
    * - '1' - without printing
    */
    public OptionVATinReceipt OptionVATinReceipt;
    public OptionVATinReceipt getOptionVATinReceipt() {
       return OptionVATinReceipt;
    }
    protected void setOptionVATinReceipt(OptionVATinReceipt value) {
       OptionVATinReceipt = value;
    }

   /**
    *(Format) 1 symbol with value: 
    * - '1' - Detailed 
    * - '0' - Brief
    */
    public OptionReceiptFormat OptionReceiptFormat;
    public OptionReceiptFormat getOptionReceiptFormat() {
       return OptionReceiptFormat;
    }
    protected void setOptionReceiptFormat(OptionReceiptFormat value) {
       OptionReceiptFormat = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - initiated payment 
    * - '1' - not initiated payment
    */
    public OptionInitiatedPayment OptionInitiatedPayment;
    public OptionInitiatedPayment getOptionInitiatedPayment() {
       return OptionInitiatedPayment;
    }
    protected void setOptionInitiatedPayment(OptionInitiatedPayment value) {
       OptionInitiatedPayment = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - finalized payment 
    * - '1' - not finalized payment
    */
    public OptionFinalizedPayment OptionFinalizedPayment;
    public OptionFinalizedPayment getOptionFinalizedPayment() {
       return OptionFinalizedPayment;
    }
    protected void setOptionFinalizedPayment(OptionFinalizedPayment value) {
       OptionFinalizedPayment = value;
    }

   /**
    *1 symbol with value: 
    *- '0' - No 
    *- '1' - Yes
    */
    public OptionPowerDownInReceipt OptionPowerDownInReceipt;
    public OptionPowerDownInReceipt getOptionPowerDownInReceipt() {
       return OptionPowerDownInReceipt;
    }
    protected void setOptionPowerDownInReceipt(OptionPowerDownInReceipt value) {
       OptionPowerDownInReceipt = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - standard receipt 
    * - '1' - invoice (client) receipt
    */
    public OptionClientReceipt OptionClientReceipt;
    public OptionClientReceipt getOptionClientReceipt() {
       return OptionClientReceipt;
    }
    protected void setOptionClientReceipt(OptionClientReceipt value) {
       OptionClientReceipt = value;
    }

   /**
    *Up to 11 symbols the amount of the due change in the 
    *stated payment type
    */
    public Double ChangeAmount;
    public Double getChangeAmount() {
       return ChangeAmount;
    }
    protected void setChangeAmount(Double value) {
       ChangeAmount = value;
    }

   /**
    *1 symbols with value:  
    *
    *
    * - '0' - Change In Cash 
    * - '1' - Same As The payment 
    * - '2' - Change In Currency
    */
    public OptionChangeType OptionChangeType;
    public OptionChangeType getOptionChangeType() {
       return OptionChangeType;
    }
    protected void setOptionChangeType(OptionChangeType value) {
       OptionChangeType = value;
    }

   /**
    *Up to 11 symbols for alte taxe amount
    */
    public Double AlteTaxeValue;
    public Double getAlteTaxeValue() {
       return AlteTaxeValue;
    }
    protected void setAlteTaxeValue(Double value) {
       AlteTaxeValue = value;
    }
}
