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
	
	/**
	 * Updates the products to the passed list. 
	 * This method should be use, for example when synchronizing 
	 * from a remote server to the local database. 
	 * This method deleted products that are not found in the argument list, 
	 * thus a faithful copy of the list is realized.
	 * Also the input is NOT validated.
	 */
	IStatus synchronize(List<Product> modelList);
}
