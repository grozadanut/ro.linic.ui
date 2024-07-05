package ro.linic.ui.pos.cloud.services;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;

import ro.linic.ui.pos.cloud.model.CloudReceipt;
import ro.linic.ui.pos.cloud.model.CloudReceiptLine;

public interface RemoteSyncer {
	/**
	 * Synchronizes the lines to the remote server.
	 * 
	 * @param lines in parameter, lines to sync
	 * @param out out parameter, this is populated with the response from server, if any
	 * @return result of the synchronization
	 */
	IStatus syncReceiptLines(Collection<CloudReceiptLine> lines, Collection out);
	
	/**
	 * Synchronizes the receipts to the remote server.
	 * 
	 * @param receipts in parameter, receipts to sync
	 * @param out out parameter, this is populated with the response from server, if any
	 * @return result of the synchronization
	 */
	IStatus syncReceipts(Collection<CloudReceipt> receipts, Collection out);
}
