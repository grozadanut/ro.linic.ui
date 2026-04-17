package ro.linic.ui.pos.driver.zfplab.internal;
public class CurrentRecInfoRes {
   /**
    *1 symbol with value '1' for initiated (opened) receipt
    */
    public String ParamOpenRec;
    public String getParamOpenRec() {
       return ParamOpenRec;
    }
    protected void setParamOpenRec(String value) {
       ParamOpenRec = value;
    }

   /**
    *3 symbols for number of sales
    */
    public String NoSales;
    public String getNoSales() {
       return NoSales;
    }
    protected void setNoSales(String value) {
       NoSales = value;
    }

   /**
    *1..11 symbols for subtotal by VAT group A
    */
    public Double SubtotalAmountVATGA;
    public Double getSubtotalAmountVATGA() {
       return SubtotalAmountVATGA;
    }
    protected void setSubtotalAmountVATGA(Double value) {
       SubtotalAmountVATGA = value;
    }

   /**
    *1..11 symbols for subtotal by VAT group B
    */
    public Double SubtotalAmountVATGB;
    public Double getSubtotalAmountVATGB() {
       return SubtotalAmountVATGB;
    }
    protected void setSubtotalAmountVATGB(Double value) {
       SubtotalAmountVATGB = value;
    }

   /**
    *1..11 symbols for subtotal by VAT group C
    */
    public Double SubtotalAmountVATGC;
    public Double getSubtotalAmountVATGC() {
       return SubtotalAmountVATGC;
    }
    protected void setSubtotalAmountVATGC(Double value) {
       SubtotalAmountVATGC = value;
    }

   /**
    *1..11 symbols for subtotal by VAT group D
    */
    public Double SubtotalAmountVATGD;
    public Double getSubtotalAmountVATGD() {
       return SubtotalAmountVATGD;
    }
    protected void setSubtotalAmountVATGD(Double value) {
       SubtotalAmountVATGD = value;
    }

   /**
    *1..11 symbols for subtotal by VAT group E
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
    public OptionParamForbiddenVoid OptionParamForbiddenVoid;
    public OptionParamForbiddenVoid getOptionParamForbiddenVoid() {
       return OptionParamForbiddenVoid;
    }
    protected void setOptionParamForbiddenVoid(OptionParamForbiddenVoid value) {
       OptionParamForbiddenVoid = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - with printing 
    * - '1' - without printing
    */
    public OptionParamVATinReceipt OptionParamVATinReceipt;
    public OptionParamVATinReceipt getOptionParamVATinReceipt() {
       return OptionParamVATinReceipt;
    }
    protected void setOptionParamVATinReceipt(OptionParamVATinReceipt value) {
       OptionParamVATinReceipt = value;
    }

   /**
    *1 symbol with value:  
    * - '0' - brief 
    * - '1' - detailed format
    */
    public OptionParamDetailedReceipt OptionParamDetailedReceipt;
    public OptionParamDetailedReceipt getOptionParamDetailedReceipt() {
       return OptionParamDetailedReceipt;
    }
    protected void setOptionParamDetailedReceipt(OptionParamDetailedReceipt value) {
       OptionParamDetailedReceipt = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - initiated payment 
    * - '1' - not initiated payment
    */
    public OptionParamInitiatedPayment OptionParamInitiatedPayment;
    public OptionParamInitiatedPayment getOptionParamInitiatedPayment() {
       return OptionParamInitiatedPayment;
    }
    protected void setOptionParamInitiatedPayment(OptionParamInitiatedPayment value) {
       OptionParamInitiatedPayment = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - finalized payment 
    * - '1' - not finalized payment
    */
    public OptionParamFinalizedPayment OptionParamFinalizedPayment;
    public OptionParamFinalizedPayment getOptionParamFinalizedPayment() {
       return OptionParamFinalizedPayment;
    }
    protected void setOptionParamFinalizedPayment(OptionParamFinalizedPayment value) {
       OptionParamFinalizedPayment = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - no power down 
    * - '1' - power down
    */
    public OptionFlagPowerDown OptionFlagPowerDown;
    public OptionFlagPowerDown getOptionFlagPowerDown() {
       return OptionFlagPowerDown;
    }
    protected void setOptionFlagPowerDown(OptionFlagPowerDown value) {
       OptionFlagPowerDown = value;
    }

   /**
    *1 symbol with value: 
    * - '0' - standard receipt 
    * - '1' - invoice (client) receipt
    */
    public OptionParamClientReceipt OptionParamClientReceipt;
    public OptionParamClientReceipt getOptionParamClientReceipt() {
       return OptionParamClientReceipt;
    }
    protected void setOptionParamClientReceipt(OptionParamClientReceipt value) {
       OptionParamClientReceipt = value;
    }

   /**
    *1..11 symbols the amount of the due change in the stated 
    *payment type
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
    *1..11 symbols for alte taxe amount
    */
    public Double AlteTaxeValue;
    public Double getAlteTaxeValue() {
       return AlteTaxeValue;
    }
    protected void setAlteTaxeValue(Double value) {
       AlteTaxeValue = value;
    }
}
