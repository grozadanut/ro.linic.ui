package ro.linic.ui.pos.base.services;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import ro.linic.ui.pos.base.model.Receipt;

public interface ReceiptUpdater {
	/**
	 * Only creates the Receipt. Does NOT create the lines, 
	 * if  the receipt has some!!!
	 */
	IStatus create(Receipt r);
	/**
	 * Does not update containing lines.
	 */
	IStatus update(Receipt r);
	IStatus delete(List<Long> ids);
	IStatus closeReceipt(Long id);
}
