package ro.linic.ui.pos.base.services;

import java.util.List;
import java.util.Optional;

import ro.linic.ui.pos.base.model.Product;

public interface ProductDataLoader {
	List<Product> findAll();
	List<Product> findByIdIn(List<Long> ids);
	
	default Optional<Product> findById(final long id) {
		return findByIdIn(List.of(id)).stream().findFirst();
	}
}
