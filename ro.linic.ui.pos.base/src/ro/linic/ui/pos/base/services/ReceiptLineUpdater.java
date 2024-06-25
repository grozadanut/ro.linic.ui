package ro.linic.ui.pos.base.services;

import java.math.BigDecimal;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import ro.linic.ui.pos.base.model.ReceiptLine;

public interface ReceiptLineUpdater {
	/**
	 * Also updates Product.quantity
	 */
	IStatus create(ReceiptLine model);
	/**
	 * Does NOT update quantity. Use updateQuantity for that. 
	 */
	IStatus update(ReceiptLine model);
	/**
	 * Also updates Product.quantity
	 */
	IStatus updateQuantity(long id, BigDecimal newQuantity);
	/**
	 * Note that if all lines are deleted for a receipt, 
	 * this method will also delete the receipt.
	 */
	IStatus delete(Set<Long> ids);
}
