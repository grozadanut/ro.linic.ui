package ro.linic.ui.pos.base.model;

import static ro.flexbiz.util.commons.NumberUtils.nullsafe;

import java.math.BigDecimal;

/**
 * @param chargeIndicator Use "true" when informing about Charges and "false" when informing about Allowances. Example value: false
 * @param amount The amount of an allowance or a charge. Must be rounded to maximum 2 decimals. Example value: 200
 */
public record AllowanceCharge(boolean chargeIndicator, BigDecimal amount) {
	public static final String CHARGE_INDICATOR_FIELD = "chargeIndicator";
	public static final String AMOUNT_FIELD = "amount";
	
	/**
	 * Returns a properly signed amount that can be used by simply adding it to the total.
	 * 
	 * @return negative amount if this is an allowance, positive if this is a charge
	 */
	public BigDecimal amountWithSign() {
		return chargeIndicator ? nullsafe(amount).abs() : nullsafe(amount).abs().negate();
	}
	
	public BigDecimal amountAbs() {
		return nullsafe(amount).abs();
	}
}
