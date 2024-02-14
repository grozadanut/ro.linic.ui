package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.wizard.Wizard;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.AcceptLicensesWizardPage;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An update wizard that is invoked when there is only one thing to update, only
 * one update to choose, and the resolution is known to be successful.
 *
 * @since 3.6
 */
public class UpdateSingleIUWizard extends Wizard {

	UpdateSingleIUPage mainPage;
	ProvisioningUI ui;
	UpdateOperation operation;
	private IEclipseContext ctx;

	public static boolean validFor(final UpdateOperation operation) {
		return operation.hasResolved() && operation.getResolutionResult().isOK()
				&& operation.getSelectedUpdates().length == 1;
	}

	public UpdateSingleIUWizard(final IEclipseContext ctx, final ProvisioningUI ui, final UpdateOperation operation) {
		super();
		this.ctx = ctx;
		this.ui = ui;
		this.operation = operation;
		setWindowTitle(ProvUIMessages.UpdateIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ctx, ProvUIImages.WIZARD_BANNER_UPDATE));
	}

	protected UpdateSingleIUPage createMainPage() {
		mainPage = new UpdateSingleIUPage(ctx, operation, ui);
		return mainPage;
	}

	@Override
	public void addPages() {
		mainPage = createMainPage();
		addPage(mainPage);

		if (!WizardWithLicenses.canBypassLicensePage()) {
			final AcceptLicensesWizardPage page = createLicensesPage();
			page.update(null, operation);
			if (page.hasLicensesToAccept())
				addPage(page);
		}
	}

	protected AcceptLicensesWizardPage createLicensesPage() {
		return new AcceptLicensesWizardPage(ctx, ui.getLicenseManager(), null, operation);
	}

	@Override
	public boolean performFinish() {
		return mainPage.performFinish();
	}

}
