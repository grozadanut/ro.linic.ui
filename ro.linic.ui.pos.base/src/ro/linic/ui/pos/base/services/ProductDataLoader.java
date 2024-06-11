package ro.linic.ui.pos.base.services;

import java.util.List;
import java.util.Optional;

import ro.linic.ui.pos.base.model.Product;

public interface ProductDataLoader {
	List<Product> findAll();
	List<Product> findByIdIn(List<Long> ids);
	String autoSku();
	/**
	 * @param identifier could be either id, sku, barcode in this order
	 * @return
	 */
	Optional<Product> findByIdentifier(String identifier);
	
	default Optional<Product> findById(final long id) {
		return findByIdIn(List.of(id)).stream().findFirst();
	}
}
