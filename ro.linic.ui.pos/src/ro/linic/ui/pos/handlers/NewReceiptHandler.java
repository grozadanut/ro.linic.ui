 
package ro.linic.ui.pos.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import ro.linic.ui.pos.parts.ReceiptPart;

public class NewReceiptHandler {
	@Execute
	public void execute(final EPartService partService) {
		partService.showPart(partService.createPart(ReceiptPart.PART_DESCRIPTOR_ID), PartState.ACTIVATE);
	}
}