 
package ro.linic.ui.pos.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.parts.ReceiptPart;

public class CloseReceiptHandler {
	@Execute
	public void execute(final EPartService partService) {
		((ReceiptPart) partService.getActivePart().getObject()).closeReceipt(PaymentType.CASH);
	}

	@CanExecute
	public boolean canExecute(final EPartService partService) {
		final MPart activePart = partService.getActivePart();
		
		if (activePart != null && activePart.getObject() instanceof ReceiptPart)
			return ((ReceiptPart) activePart.getObject()).canCloseReceipt();
		
		return false;
	}
}