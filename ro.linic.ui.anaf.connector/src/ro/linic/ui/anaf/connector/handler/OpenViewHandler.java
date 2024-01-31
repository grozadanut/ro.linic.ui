 
package ro.linic.ui.anaf.connector.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import ro.linic.ui.anaf.connector.part.AdditionalInformationPart;

public class OpenViewHandler {
	@Execute
	public void execute(final EPartService partService) {
		partService.showPart(partService.createPart(AdditionalInformationPart.PART_DESCRIPTOR_ID),
				PartState.VISIBLE);
	}
}