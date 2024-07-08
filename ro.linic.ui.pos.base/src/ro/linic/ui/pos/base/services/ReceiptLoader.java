package ro.linic.ui.pos.base.services;

import java.util.List;
import java.util.Optional;

import ro.linic.ui.pos.base.model.Receipt;

public interface ReceiptLoader {
	/**
	 * Returns Receipts loaded with the lines as well.
	 */
	List<Receipt> findAll();
	/**
	 * Returns Receipts loaded with the lines as well.
	 */
	List<Receipt> findByIdIn(List<Long> ids);
	List<Receipt> findUnclosed();
	List<Receipt> findClosed();
	
	/**
	 * Returns a Receipt loaded with the lines as well.
	 */
	default Optional<Receipt> findById(final long id) {
		return findByIdIn(List.of(id)).stream().findFirst();
	}
}
