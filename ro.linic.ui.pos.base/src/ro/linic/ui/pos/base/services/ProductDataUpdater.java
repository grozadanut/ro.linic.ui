package ro.linic.ui.pos.base.services;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import ro.linic.ui.pos.base.model.Product;

public interface ProductDataUpdater {
	IStatus create(Product p);
	/**
	 * Does not update stock. Use {@code updateStock()} for that
	 */
	IStatus update(Product p);
	IStatus updateStock(long id, BigDecimal stock);
	IStatus delete(List<Long> ids);
}
