package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.ProvisioningUI;

public class InstallWizardPage extends SizeComputingWizardPage {

	public InstallWizardPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard,
			final IUElementListRoot root, final ProfileChangeOperation operation) {
		super(ctx, ui, wizard, root, operation);
		setTitle(ProvUIMessages.InstallWizardPage_Title);
		setDescription(ProvUIMessages.InstallWizardPage_NoCheckboxDescription);
	}

	@Override
	protected String getOperationLabel() {
		return ProvUIMessages.InstallIUOperationLabel;
	}

	@Override
	protected String getOperationTaskName() {
		return ProvUIMessages.InstallIUOperationTask;
	}

}
