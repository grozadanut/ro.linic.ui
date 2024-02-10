package ro.linic.ui.legacy.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class InchideBonWizardDialog extends WizardDialog
{
	public InchideBonWizardDialog(final Shell parentShell, final IWizard newWizard)
	{
		super(parentShell, newWizard);
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(Display.getCurrent().getClientArea().width, 360);
	}
	
	@Override
	protected Control createButtonBar(final Composite parent)
	{
		buttonBar = super.createButtonBar(parent);
		final GridData gd = new GridData();
		gd.exclude = true;
		buttonBar.setLayoutData(gd);
		return buttonBar;
	}
	
	@Override
	public void cancelPressed()
	{
		super.cancelPressed();
	}
	
	@Override
	public void nextPressed()
	{
		super.nextPressed();
	}
	
	@Override
	public void finishPressed()
	{
		super.finishPressed();
	}
}
