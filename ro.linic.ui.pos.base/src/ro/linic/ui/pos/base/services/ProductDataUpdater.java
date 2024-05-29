package ro.linic.ui.pos.base.services;

import java.math.BigDecimal;

import ro.linic.ui.pos.base.model.Product;

public interface ProductDataUpdater {
	void create(Product p);
	/**
	 * Does not update stock. Use {@code updateStock()} for that
	 */
	void update(Product p);
	void updateStock(long id, BigDecimal stock);
	void delete(Product p);
}
