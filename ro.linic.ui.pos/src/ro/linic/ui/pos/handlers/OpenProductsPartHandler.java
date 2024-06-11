package ro.linic.ui.pos.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import ro.linic.ui.pos.parts.ProductsPart;

public class OpenProductsPartHandler {
	@Execute
	public void execute(final EPartService partService) {
		partService.showPart(ProductsPart.PART_ID, PartState.ACTIVATE);
	}
}
