package ro.linic.ui.pos.base.model;

import static ro.linic.util.commons.NumberUtils.add;
import static ro.linic.util.commons.NumberUtils.multiply;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @param id unique id of this line
 * @param sku code of the sold item
 * @param name A name for a sold item.
 * @param uom unit of measure
 * @param quantity The quantity of items (goods or services) that is charged, rounded to 3 decimal places.
 * @param price The price of an item, exclusive of allowances or charges, rounded to 2 decimal places. Item price can not be negative.
 * @param allowanceCharge Line level allowanceCharge applied to the price.
 * @param taxCode tax code as specified in the ecr. eg: 1
 * @param departmentCode department code as specified in the ecr. eg: 1
 */
public record ReceiptLine(Long id, String sku, String name, String uom, BigDecimal quantity, BigDecimal price, AllowanceCharge allowanceCharge,
		String taxCode, String departmentCode) {
	
	/**
	 * @return total including allowance or charge amount
	 */
	public BigDecimal total() {
		return add(multiply(quantity, price),
				Optional.ofNullable(allowanceCharge)
				.map(AllowanceCharge::amountWithSign)
				.orElse(null));
	}
}
