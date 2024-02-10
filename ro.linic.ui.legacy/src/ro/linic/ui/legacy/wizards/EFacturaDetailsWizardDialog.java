package ro.linic.ui.legacy.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class EFacturaDetailsWizardDialog extends WizardDialog
{
	public EFacturaDetailsWizardDialog(final Shell parentShell, final IWizard newWizard)
	{
		super(parentShell, newWizard);
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(650, 500);
	}
	
	@Override
	public void nextPressed()
	{
		((EFacturaBaseWizard) getWizard()).nextPressed();
		super.nextPressed();
	}
	
	@Override
	protected void backPressed()
	{
		((EFacturaBaseWizard) getWizard()).backPressed();
		super.backPressed();
	}
}
