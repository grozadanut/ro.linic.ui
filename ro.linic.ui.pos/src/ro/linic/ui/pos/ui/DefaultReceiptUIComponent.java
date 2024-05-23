package ro.linic.ui.pos.ui;

import org.eclipse.swt.widgets.Composite;

import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.ReceiptUIComponent;

public class DefaultReceiptUIComponent implements ReceiptUIComponent {

	@Override
	public void postConstruct(final Composite parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canCloseReceipt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void closeReceipt(final PaymentType paymentType) {
		// TODO Auto-generated method stub

	}
}
