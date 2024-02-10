 
package ro.linic.ui.legacy.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import ro.linic.ui.legacy.parts.components.VanzareInterface;
import ro.linic.ui.legacy.wizards.InchideBonWizard.TipInchidere;

public class InchideBonHandler
{
	@Execute
	public void execute(final EPartService partService)
	{
		((VanzareInterface) partService.getActivePart().getObject()).closeBon(TipInchidere.PRIN_CASA);
	}

	@CanExecute
	public boolean canExecute(final EPartService partService)
	{
		final MPart activePart = partService.getActivePart();
		
		if (activePart != null && activePart.getObject() instanceof VanzareInterface)
			return ((VanzareInterface) activePart.getObject()).getBonCasa() != null;
		
		return false;
	}
}