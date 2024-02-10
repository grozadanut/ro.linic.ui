package ro.linic.ui.legacy.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class CustomerDebtWizardDialog extends WizardDialog
{
	public CustomerDebtWizardDialog(final Shell parentShell, final IWizard newWizard)
	{
		super(parentShell, newWizard);
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(630, 730);
	}
	
	@Override
	public void nextPressed()
	{
		((CustomerDebtWizard) getWizard()).nextPressed();
		super.nextPressed();
	}
	
	@Override
	protected void backPressed()
	{
		((CustomerDebtWizard) getWizard()).backPressed();
		super.backPressed();
	}
}
