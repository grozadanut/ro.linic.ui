package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.ProvisioningUI;

public class RemediationPage extends ResolutionStatusPage {

	private RemediationGroup remediationGroup;

	private Composite mainComposite;

	protected RemediationPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard, final IUElementListRoot input, final ProfileChangeOperation operation) {
		super(ctx, "RemediationPage", ui, wizard); //$NON-NLS-1$
		if (wizard instanceof InstallWizard) {
			setTitle(ProvUIMessages.InstallRemediationPage_Title);
			setDescription(ProvUIMessages.InstallRemediationPage_Description);
		} else {
			setTitle(ProvUIMessages.UpdateRemediationPage_Title);
			setDescription(ProvUIMessages.UpdateRemediationPage_Description);
		}
	}

	@Override
	public void createControl(final Composite parent) {
		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());

		remediationGroup = new RemediationGroup(ctx, this);
		remediationGroup.createRemediationControl(mainComposite);
		final Composite remediationComposite = remediationGroup.getComposite();
		setMessage(remediationGroup.getMessage(), IStatus.WARNING);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		remediationComposite.setLayoutData(gd);
		setControl(mainComposite);
		setPageComplete(false);

		Dialog.applyDialogFont(mainComposite);
	}

	public RemediationGroup getRemediationGroup() {
		return remediationGroup;
	}

	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete() && remediationGroup.getCurrentRemedy() != null;
	}

	public void updateStatus(final IUElementListRoot newRoot, final ProfileChangeOperation operation, final Object[] planSelections) {
		remediationGroup.update(((ProvisioningOperationWizard) getWizard()).getRemediationOperation());
		setDetailText(operation);
	}

	@Override
	protected void updateCaches(final IUElementListRoot root, final ProfileChangeOperation resolvedOperation) {
		// TODO Auto-generated method stub
	}

	@Override
	protected boolean isCreated() {
		return false;
	}

	@Override
	protected IUDetailsGroup getDetailsGroup() {
		return remediationGroup.getDetailsGroup();
	}

	@Override
	protected IInstallableUnit getSelectedIU() {
		// Not applicable
		return null;
	}

	@Override
	protected Object[] getSelectedElements() {
		return new Object[] {};
	}

	@Override
	protected String getDialogSettingsName() {
		return null;
	}

	@Override
	protected SashForm getSashForm() {
		return null;
	}

	@Override
	protected int getColumnWidth(final int index) {
		return 0;
	}

	@Override
	protected String getClipboardText(final Control control) {
		return null;
	}
}