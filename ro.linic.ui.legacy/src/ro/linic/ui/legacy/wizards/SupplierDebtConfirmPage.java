package ro.linic.ui.legacy.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import ro.linic.ui.legacy.session.UIUtils;

public class SupplierDebtConfirmPage extends WizardPage
{
	private Text resultDescription;
	
	public SupplierDebtConfirmPage()
	{
        super("Confirma");
        setTitle("Confirmati");
        setMessage("Aceste documente vor fi create. Continuati?");
    }
	
	@Override
	public void createControl(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		
		resultDescription = new Text(container, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		resultDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		UIUtils.setFont(resultDescription);
		
		setControl(container);
		setPageComplete(false);
	}
	
	public SupplierDebtConfirmPage resultDescription(final String description)
	{
		resultDescription.setText(description);
		return this;
	}
}
