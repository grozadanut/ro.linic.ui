 
package ro.linic.ui.base.handler;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class SaveAllHandler {
	@CanExecute
	boolean canExecute(@Optional final EPartService partService) {
		if (partService != null) {
			return !partService.getDirtyParts().isEmpty();
		}
		return false;
	}
	
	@Execute
	public void execute(final EPartService partService) {
		partService.saveAll(true);
	}
}