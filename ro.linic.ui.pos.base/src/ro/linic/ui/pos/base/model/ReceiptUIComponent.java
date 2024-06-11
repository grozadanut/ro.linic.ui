package ro.linic.ui.pos.base.model;

import org.eclipse.swt.widgets.Composite;

/**
 * This is the interface the needs to be implemented if you want to contribute to the ReceiptPart UI.
 * To contribute your own UI you need to add a receiptUIComponent extension point. 
 * The implementation will then be chosen based on the priority.
 */
public interface ReceiptUIComponent {
	void postConstruct(Composite parent);
	boolean canCloseReceipt();
	void closeReceipt(PaymentType paymentType);
	void setFocus();
	void persistState();
}
