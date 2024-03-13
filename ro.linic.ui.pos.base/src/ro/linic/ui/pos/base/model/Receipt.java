package ro.linic.ui.pos.base.model;

import static ro.linic.util.commons.NumberUtils.add;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

/**
 * @param lines that compose this Receipt
 * @param allowanceCharge that applies to the receipt as a whole, after subtotal
 */
public record Receipt(Collection<ReceiptLine> lines, AllowanceCharge allowanceCharge) {
	
	/**
	 * @return total including allowance or charge amount
	 */
	public BigDecimal total() {
		return lines.stream()
				.map(ReceiptLine::total)
				.reduce(BigDecimal::add)
				.map(subtotal -> add(subtotal, Optional.ofNullable(allowanceCharge)
						.map(AllowanceCharge::amountWithSign)
						.orElse(null)))
				.orElse(BigDecimal.ZERO);
	}
}
