package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.wizard.IWizardPage;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.AvailableIUElement;
import ro.linic.ui.p2.internal.ui.model.AvailableUpdateElement;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * @since 3.4
 */
public class UpdateWizard extends WizardWithLicenses {
	IInstallableUnit[] iusToReplace;
	boolean skipSelectionsPage = false;
	IUElementListRoot firstPageRoot;
	Update[] initialSelections;

	public static Collection<IInstallableUnit> getIUsToReplace(final Object[] elements) {
		final Set<IInstallableUnit> iusToReplace = new HashSet<>();
		for (final Object element : elements) {
			if (element instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) element).getIUToBeUpdated());
			}
		}
		return iusToReplace;
	}

	public static IInstallableUnit[] getReplacementIUs(final Object[] elements) {
		final Set<IInstallableUnit> replacements = new HashSet<>();
		for (final Object element : elements) {
			if (element instanceof AvailableUpdateElement) {
				replacements.add(((AvailableUpdateElement) element).getIU());
			}
		}
		return replacements.toArray(new IInstallableUnit[replacements.size()]);
	}

	public static Update[] makeUpdatesFromElements(final Object[] elements) {
		final Set<Update> updates = new HashSet<>();
		for (final Object element : elements) {
			if (element instanceof AvailableUpdateElement) {
				updates.add(((AvailableUpdateElement) element).getUpdate());
			}
		}
		return updates.toArray(new Update[updates.size()]);
	}

	/**
	 * Open an update wizard. For update wizards, the operation must have been
	 * resolved in advanced. This prevents searching for updates in the UI thread.
	 *
	 * @param ui                the provisioning UI
	 * @param operation         the update operation. Must already be resolved!
	 * @param initialSelections initial selections for the wizard (can be null)
	 * @param preloadJob        a job that has been used to preload metadata
	 *                          repositories (can be null)
	 */
	public UpdateWizard(final IEclipseContext ctx, final ProvisioningUI ui, final UpdateOperation operation, final Object[] initialSelections,
			final LoadMetadataRepositoryJob preloadJob) {
		super(ctx, ui, operation, initialSelections, preloadJob);
		this.initialSelections = (Update[]) initialSelections;
		Assert.isLegal(operation.hasResolved(), "Cannot create an update wizard on an unresolved operation"); //$NON-NLS-1$
		setWindowTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ctx, ProvUIImages.WIZARD_BANNER_UPDATE));
	}

	private boolean isLocked(final IProfile profile, final IInstallableUnit iuToBeUpdated) {
		return Boolean.parseBoolean(profile.getInstallableUnitProperty(iuToBeUpdated, IProfile.PROP_PROFILE_LOCKED_IU));
	}

	public void deselectLockedIUs() {
		final IProfileRegistry profileRegistry = ui.getSession().getProvisioningAgent().getService(IProfileRegistry.class);
		final IProfile profile = profileRegistry.getProfile(ui.getProfileId());

		final ArrayList<Update> newSelection = new ArrayList<>(initialSelections.length);
		for (final Update initialSelection : initialSelections) {
			if (!isLocked(profile, initialSelection.toUpdate)) {
				newSelection.add(initialSelection);
			}
		}

		((UpdateOperation) operation).setSelectedUpdates(newSelection.toArray(new Update[newSelection.size()]));
		recomputePlan(getContainer());
	}

	@Override
	protected ISelectableIUsPage createMainPage(final IUElementListRoot input, final Object[] selections) {
		mainPage = new SelectableIUsPage(ctx, ui, this, getAllPossibleUpdatesRoot(), selections);
		mainPage.setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		mainPage.setDescription(ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
		((SelectableIUsPage) mainPage).updateStatus(getAllPossibleUpdatesRoot(), operation);
		return mainPage;
	}

	@Override
	protected ResolutionResultsWizardPage createResolutionPage() {
		return new UpdateWizardPage(ctx, ui, this, root, (UpdateOperation) operation);
	}

	@Override
	protected void initializeResolutionModelElements(final Object[] selectedElements) {
		if (selectedElements == null)
			return;
		root = new IUElementListRoot(ctx, ui);
		if (operation instanceof RemediationOperation) {
			final AvailableIUElement[] elements = ElementUtils
					.requestToElement(ctx, ((RemediationOperation) operation).getCurrentRemedy(), false);
			root.setChildren(elements);
			// planSelections = elements;
		} else {
			final ArrayList<AvailableUpdateElement> list = new ArrayList<>(selectedElements.length);
			final ArrayList<AvailableUpdateElement> selected = new ArrayList<>(selectedElements.length);
			for (final Object selectedElement : selectedElements) {
				if (selectedElement instanceof AvailableUpdateElement) {
					final AvailableUpdateElement element = (AvailableUpdateElement) selectedElement;
					final AvailableUpdateElement newElement = new AvailableUpdateElement(ctx, root, element.getIU(),
							element.getIUToBeUpdated(), getProfileId(), shouldShowProvisioningPlanChildren());
					list.add(newElement);
					selected.add(newElement);
				} else if (selectedElement instanceof Update) {
					final Update update = (Update) selectedElement;
					final AvailableUpdateElement newElement = new AvailableUpdateElement(ctx, root, update.replacement,
							update.toUpdate, getProfileId(), shouldShowProvisioningPlanChildren());
					list.add(newElement);
					selected.add(newElement);
				}
			}
			root.setChildren(list.toArray());
			planSelections = selected.toArray();
		}
	}

	@Override
	protected IResolutionErrorReportingPage createErrorReportingPage() {
		return (SelectableIUsPage) mainPage;
	}

	public void setSkipSelectionsPage(final boolean skipSelectionsPage) {
		this.skipSelectionsPage = skipSelectionsPage;
	}

	@Override
	public IWizardPage getStartingPage() {
		if (skipSelectionsPage) {
			// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276963
			final IWizardPage page = getNextPage(mainPage);
			if (page != null)
				return page;
		}
		return mainPage;
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(final Object[] elements) {
		if (operation == null) {
			operation = new UpdateOperation(ui.getSession(), getIUsToReplace(elements));
			operation.setProfileId(getProfileId());
			// operation.setRootMarkerKey(getRootMarkerKey());
		} else {
			((UpdateOperation) operation).setSelectedUpdates(makeUpdatesFromElements(elements));
		}
		return operation;
	}

	private IUElementListRoot getAllPossibleUpdatesRoot() {
		if (firstPageRoot == null) {
			firstPageRoot = new IUElementListRoot(ctx, ui);
			if (operation != null && operation instanceof UpdateOperation) {
				Update[] updates;
				if (getPolicy().getShowLatestVersionsOnly()) {
					updates = ((UpdateOperation) operation).getSelectedUpdates();
				} else {
					updates = ((UpdateOperation) operation).getPossibleUpdates();
				}
				final ArrayList<AvailableUpdateElement> allPossible = new ArrayList<>(updates.length);
				for (final Update update : updates) {
					final AvailableUpdateElement newElement = new AvailableUpdateElement(ctx, firstPageRoot, update.replacement,
							update.toUpdate, getProfileId(), shouldShowProvisioningPlanChildren());
					allPossible.add(newElement);
				}
				firstPageRoot.setChildren(allPossible.toArray());
			}
		}
		return firstPageRoot;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		remediationPage = new RemediationPage(ctx, ui, this, root, operation);
		return remediationPage;
	}

}
