package ro.linic.ui.legacy.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class SupplierDebtWizardDialog extends WizardDialog
{
	public SupplierDebtWizardDialog(final Shell parentShell, final IWizard newWizard)
	{
		super(parentShell, newWizard);
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(620, 620);
	}
	
	@Override
	public void nextPressed()
	{
		((SupplierDebtWizard) getWizard()).nextPressed();
		super.nextPressed();
	}
	
	@Override
	protected void backPressed()
	{
		((SupplierDebtWizard) getWizard()).backPressed();
		super.backPressed();
	}
}
