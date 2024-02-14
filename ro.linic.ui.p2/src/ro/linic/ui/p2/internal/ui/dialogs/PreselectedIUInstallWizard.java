package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.jface.wizard.IWizardPage;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.QueryableMetadataRepositoryManager;
import ro.linic.ui.p2.internal.ui.model.AvailableIUElement;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An Install wizard that is invoked when the user has already selected which
 * IUs should be installed and does not need to browse the available software.
 *
 * @since 3.5
 */
public class PreselectedIUInstallWizard extends WizardWithLicenses {

	QueryableMetadataRepositoryManager manager;

	public PreselectedIUInstallWizard(final IEclipseContext ctx, final ProvisioningUI ui, final InstallOperation operation, 
			final Collection<IInstallableUnit> initialSelections, final LoadMetadataRepositoryJob job) {
		super(ctx, ui, operation, initialSelections.toArray(), job);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ctx, ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	@Override
	public IWizardPage getStartingPage() {
		if (remediationOperation != null && remediationOperation.getResolutionResult() == Status.OK_STATUS) {
			return getNextPage(mainPage);
		}
		return super.getStartingPage();
	}

	@Override
	protected ISelectableIUsPage createMainPage(final IUElementListRoot input, final Object[] selections) {
		mainPage = new SelectableIUsPage(ctx, ui, this, input, selections);
		mainPage.setTitle(ProvUIMessages.PreselectedIUInstallWizard_Title);
		mainPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
		((SelectableIUsPage) mainPage).updateStatus(input, operation);
		return mainPage;
	}

	@Override
	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(ctx, ui, this, root, operation);
	}

	@Override
	protected void initializeResolutionModelElements(final Object[] selectedElements) {
		root = new IUElementListRoot(ctx, ui);
		final ArrayList<AvailableIUElement> list = new ArrayList<>(selectedElements.length);
		final ArrayList<AvailableIUElement> selected = new ArrayList<>(selectedElements.length);
		for (final Object selectedElement : selectedElements) {
			final IInstallableUnit iu = ElementUtils.getIU(selectedElement);
			if (iu != null) {
				final AvailableIUElement element = new AvailableIUElement(ctx, root, iu, getProfileId(), shouldShowProvisioningPlanChildren());
				list.add(element);
				selected.add(element);
			}
		}
		root.setChildren(list.toArray());
		planSelections = selected.toArray();
		if (licensePage != null) {
			licensePage.update(ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]), operation);
		}
	}

	@Override
	protected IResolutionErrorReportingPage createErrorReportingPage() {
		return (IResolutionErrorReportingPage) mainPage;
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(final Object[] elements) {
		final InstallOperation op = new InstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements));
		op.setProfileId(getProfileId());
		//		op.setRootMarkerKey(getRootMarkerKey());
		return op;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		remediationPage = new RemediationPage(ctx, ui, this, root, operation);
		return remediationPage;
	}

}
