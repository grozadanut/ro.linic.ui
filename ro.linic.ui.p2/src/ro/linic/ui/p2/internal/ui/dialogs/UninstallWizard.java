package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.jface.wizard.IWizardPage;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.internal.ui.model.InstalledIUElement;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * @since 3.4
 */
public class UninstallWizard extends ProvisioningOperationWizard {

	public UninstallWizard(final IEclipseContext ctx, final ProvisioningUI ui, final UninstallOperation operation,
			final Collection<IInstallableUnit> initialSelections, final LoadMetadataRepositoryJob job) {
		super(ctx, ui, operation, initialSelections.toArray(), job);
		setWindowTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ctx, ProvUIImages.WIZARD_BANNER_UNINSTALL));
	}

	@Override
	protected ISelectableIUsPage createMainPage(final IUElementListRoot input, final Object[] selections) {
		mainPage = new SelectableIUsPage(ctx, ui, this, input, selections);
		mainPage.setTitle(ProvUIMessages.UninstallIUOperationLabel);
		mainPage.setDescription(ProvUIMessages.UninstallDialog_UninstallMessage);
		((SelectableIUsPage) mainPage).updateStatus(input, operation);
		return mainPage;
	}

	@Override
	protected ResolutionResultsWizardPage createResolutionPage() {
		return new UninstallWizardPage(ctx, ui, this, root, (UninstallOperation) operation);
	}

	@Override
	protected void initializeResolutionModelElements(final Object[] selectedElements) {
		root = new IUElementListRoot(ctx, ui);
		final ArrayList<InstalledIUElement> list = new ArrayList<>(selectedElements.length);
		final ArrayList<InstalledIUElement> selections = new ArrayList<>(selectedElements.length);
		for (final Object selectedElement : selectedElements) {
			final IInstallableUnit iu = ElementUtils.getIU(selectedElement);
			if (iu != null) {
				final InstalledIUElement element = new InstalledIUElement(ctx, root, getProfileId(), iu);
				list.add(element);
				selections.add(element);
			}
		}
		root.setChildren(list.toArray());
		planSelections = selections.toArray();
	}

	@Override
	protected IResolutionErrorReportingPage createErrorReportingPage() {
		return (SelectableIUsPage) mainPage;
	}

	@Override
	public IWizardPage getStartingPage() {
		if (getCurrentStatus().isOK()) {
			((SelectableIUsPage) mainPage).setPageComplete(true);
			return resolutionPage;
		}
		return super.getStartingPage();
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(final Object[] elements) {
		final UninstallOperation op = new UninstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements));
		op.setProfileId(getProfileId());
		// op.setRootMarkerKey(getRootMarkerKey());
		op.setProvisioningContext(getProvisioningContext());
		return op;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		// TODO Auto-generated method stub
		return null;
	}

}
