package ro.linic.ui.pos.base.services;

import java.util.List;
import java.util.Optional;

import ro.linic.ui.pos.base.model.ReceiptLine;

public interface ReceiptLineLoader {
	/**
	 * Note: for the Receipt field it only loads the receipt.id
	 */
	List<ReceiptLine> findAll();
	/**
	 * Note: for the Receipt field it only loads the receipt.id
	 */
	List<ReceiptLine> findByIdIn(List<Long> ids);
	List<ReceiptLine> findWhere(String where);
	
	/**
	 * Note: for the Receipt field it only loads the receipt.id
	 */
	default Optional<ReceiptLine> findById(final long id) {
		return findByIdIn(List.of(id)).stream().findFirst();
	}
}
