package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.AvailableIUElement;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An install wizard that allows the users to browse all of the repositories and
 * search/select for items to install.
 *
 * @since 3.6
 */
public class InstallWizard extends WizardWithLicenses {

	SelectableIUsPage errorReportingPage;
	boolean ignoreSelectionChanges = false;
	IStatus installHandlerStatus;

	public InstallWizard(final IEclipseContext ctx, final ProvisioningUI ui, final InstallOperation operation,
			final Collection<IInstallableUnit> initialSelections, final LoadMetadataRepositoryJob preloadJob) {
		super(ctx, ui, operation, initialSelections == null ? null : initialSelections.toArray(), preloadJob);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ctx, ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	@Override
	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(ctx, ui, this, root, operation);
	}

	@Override
	protected ISelectableIUsPage createMainPage(final IUElementListRoot input, final Object[] selections) {
		mainPage = new AvailableIUsPage(ctx, ui, this);
		if (selections != null && selections.length > 0)
			mainPage.setCheckedElements(selections);
		return mainPage;

	}

	@Override
	protected void initializeResolutionModelElements(final Object[] selectedElements) {
		if (selectedElements == null)
			return;
		root = new IUElementListRoot(ctx, ui);
		if (operation instanceof RemediationOperation) {
			final AvailableIUElement[] elements = ElementUtils
					.requestToElement(ctx, ((RemediationOperation) operation).getCurrentRemedy(), true);
			root.setChildren(elements);
			planSelections = elements;
		} else {
			final ArrayList<AvailableIUElement> list = new ArrayList<>(selectedElements.length);
			final ArrayList<AvailableIUElement> selections = new ArrayList<>(selectedElements.length);
			for (final Object selectedElement : selectedElements) {
				final IInstallableUnit iu = ElementUtils.getIU(selectedElement);
				if (iu != null) {
					final AvailableIUElement element = new AvailableIUElement(ctx, root, iu, getProfileId(),
							shouldShowProvisioningPlanChildren());
					list.add(element);
					selections.add(element);
				}
			}
			root.setChildren(list.toArray());
			planSelections = selections.toArray();
		}
	}

	/*
	 * Overridden to dynamically determine which page to get selections from.
	 */
	@Override
	protected Object[] getOperationSelections() {
		return getOperationSelectionsPage().getCheckedIUElements();
	}

	/*
	 * Get the page that is driving operation selections. This is usually the main
	 * page, but it could be error page if there was a resolution error and the user
	 * decides to change selections and try again without going back.
	 */
	protected ISelectableIUsPage getOperationSelectionsPage() {
		final IWizardPage page = getContainer().getCurrentPage();
		if (page instanceof ISelectableIUsPage)
			return (ISelectableIUsPage) page;
		// return the main page if we weren't on main or error page
		return mainPage;
	}

	@Override
	protected ProvisioningContext getProvisioningContext() {
		return ((AvailableIUsPage) mainPage).getProvisioningContext();
	}

	@Override
	protected IResolutionErrorReportingPage createErrorReportingPage() {
		if (root == null)
			errorReportingPage = new SelectableIUsPage(ctx, ui, this, null, null);
		else
			errorReportingPage = new SelectableIUsPage(ctx, ui, this, root, root.getChildren(root));
		errorReportingPage.setTitle(ProvUIMessages.InstallWizardPage_Title);
		errorReportingPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
		errorReportingPage.updateStatus(root, operation);
		return errorReportingPage;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		remediationPage = new RemediationPage(ctx, ui, this, root, operation);
		return remediationPage;
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(final Object[] elements) {
		final InstallOperation op = new InstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements));
		op.setProfileId(getProfileId());
		// op.setRootMarkerKey(getRootMarkerKey());
		return op;
	}

	@Override
	protected boolean shouldUpdateErrorPageModelOnPlanChange() {
		// We don't want the root of the error page to change unless we are on the
		// main page. For example, if we are on the error page, change checkmarks, and
		// resolve again with an error, we wouldn't want the root items to change in the
		// error page.
		return getContainer().getCurrentPage() == mainPage && super.shouldUpdateErrorPageModelOnPlanChange();
	}

	@Override
	protected void planChanged() {
		super.planChanged();
		synchSelections(getOperationSelectionsPage());
	}

	/*
	 * overridden to ensure that the main page selections stay in synch with changes
	 * to the error page.
	 */
	@Override
	public void operationSelectionsChanged(final ISelectableIUsPage page) {
		if (ignoreSelectionChanges)
			return;
		super.operationSelectionsChanged(page);
		// If we are on the error page, resolution has failed.
		// Our ability to move on depends on whether the selections have changed.
		// If they are the same selections, then we are not complete until selections
		// are changed.
		if (getOperationSelectionsPage() == errorPage)
			((WizardPage) errorPage).setPageComplete(
					pageSelectionsHaveChanged(errorPage) && errorPage.getCheckedIUElements().length > 0);
		synchSelections(page);
	}

	private void synchSelections(final ISelectableIUsPage triggeringPage) {
		// We don't want our programmatic changes to cause all this to happen again
		ignoreSelectionChanges = true;
		try {
			if (triggeringPage == errorReportingPage) {
				mainPage.setCheckedElements(triggeringPage.getCheckedIUElements());
			} else if (triggeringPage == mainPage) {
				errorReportingPage.setCheckedElements(triggeringPage.getCheckedIUElements());
			}
		} finally {
			ignoreSelectionChanges = false;
		}
	}

	/*
	 * Overridden to check whether there are UpdateManager install handlers in the
	 * item to be installed. Operations don't know about this compatibility issue.
	 */
	@Override
	public IStatus getCurrentStatus() {
		final IStatus originalStatus = super.getCurrentStatus();
		final int sev = originalStatus.getSeverity();
		// Use the previously computed status if the user cancelled or if we were
		// already in error.
		// If we don't have an operation or a plan, we can't check this condition
		// either, so just
		// use the normal status.
		if (sev == IStatus.CANCEL || sev == IStatus.ERROR || operation == null
				|| operation.getProvisioningPlan() == null) {
			return originalStatus;
		}
		// Does the plan require install handler support?
//		installHandlerStatus = UpdateManagerCompatibility.getInstallHandlerStatus(operation.getProvisioningPlan());
//		if (!installHandlerStatus.isOK()) {
//			// Set the status into the wizard. This ensures future calls to this method
//			// won't
//			// repeat the work (and prompting).
//			couldNotResolveStatus = installHandlerStatus;
//
//			// Is the update manager installer present? If so, offer to open it.
//			// In either case, the failure will be reported in this wizard.
//			if (ProvUI.isUpdateManagerInstallerPresent()) {
//				PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
//					final Shell shell = ProvUI.getDefaultParentShell();
//					final MessageDialog dialog = new MessageDialog(shell, ProvUIMessages.Policy_RequiresUpdateManagerTitle,
//							null, ProvUIMessages.Policy_RequiresUpdateManagerMessage, MessageDialog.WARNING,
//							new String[] { ProvUIMessages.LaunchUpdateManagerButton, IDialogConstants.CANCEL_LABEL },
//							0);
//					if (dialog.open() == 0)
//						BusyIndicator.showWhile(shell.getDisplay(), UpdateManagerCompatibility::openInstaller);
//				});
//			}
//			return installHandlerStatus;
//		}
		return originalStatus;
	}

	/*
	 * When we've found an install handler, that status trumps anything that the
	 * operation might have determined. We are relying here on the knowledge that
	 * the wizard's couldNotResolveStatus is reset on every new resolution, so that
	 * status only holds the installHandler status when it is the current status.
	 * The installHandlerStatus must be non-OK for it to matter at all.
	 *
	 */
	@Override
	public boolean statusOverridesOperation() {
		return super.statusOverridesOperation() || (installHandlerStatus != null && !installHandlerStatus.isOK()
				&& couldNotResolveStatus == installHandlerStatus);
	}
}
