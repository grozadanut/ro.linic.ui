package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.operations.UpdateOperation;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.ProvisioningUI;

public class UpdateWizardPage extends SizeComputingWizardPage {

	public UpdateWizardPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard,
			final IUElementListRoot root, final UpdateOperation operation) {
		super(ctx, ui, wizard, root, operation);
		setTitle(ProvUIMessages.UpdateWizardPage_Title);
		setDescription(ProvUIMessages.UpdateWizardPage_Description);
	}

	@Override
	protected String getIUDescription(final IInstallableUnit iu) {
		if (iu != null) {
			final IUpdateDescriptor updateDescriptor = iu.getUpdateDescriptor();
			if (updateDescriptor != null && updateDescriptor.getDescription() != null && updateDescriptor.getDescription().length() > 0)
				return updateDescriptor.getDescription();
		}
		return super.getIUDescription(iu);
	}

	@Override
	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}

	@Override
	protected String getOperationTaskName() {
		return ProvUIMessages.UpdateIUOperationTask;
	}
}
