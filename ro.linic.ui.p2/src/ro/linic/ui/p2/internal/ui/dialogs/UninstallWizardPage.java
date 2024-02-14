package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.ProvisioningUI;

public class UninstallWizardPage extends ResolutionResultsWizardPage {

	public UninstallWizardPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard,
			final IUElementListRoot root, final UninstallOperation initialResolution) {
		super(ctx, ui, wizard, root, initialResolution);
		setTitle(ProvUIMessages.UninstallWizardPage_Title);
		setDescription(ProvUIMessages.UninstallWizardPage_Description);
	}

	@Override
	protected String getOperationLabel() {
		return ProvUIMessages.UninstallIUOperationLabel;
	}

	@Override
	protected String getOperationTaskName() {
		return ProvUIMessages.UninstallIUOperationTask;
	}

	@Override
	protected IQueryable<IInstallableUnit> getQueryable(final IProvisioningPlan plan) {
		return plan.getRemovals();
	}
}
