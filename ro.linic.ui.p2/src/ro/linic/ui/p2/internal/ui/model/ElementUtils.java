package ro.linic.ui.p2.internal.ui.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Remedy;
import org.eclipse.equinox.p2.operations.RemedyIUDetail;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Utility methods for manipulating model elements.
 *
 * @since 3.4
 *
 */
public class ElementUtils {

	public static void updateRepositoryUsingElements(final ProvisioningUI ui, final MetadataRepositoryElement[] elements) {
		ui.signalRepositoryOperationStart();
		final IMetadataRepositoryManager metaManager = ProvUI.getMetadataRepositoryManager(ui.getSession());
		final IArtifactRepositoryManager artManager = ProvUI.getArtifactRepositoryManager(ui.getSession());
		try {
			final int visibilityFlags = ui.getRepositoryTracker().getMetadataRepositoryFlags();
			final URI[] currentlyEnabled = metaManager.getKnownRepositories(visibilityFlags);
			final URI[] currentlyDisabled = metaManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | visibilityFlags);
			for (final MetadataRepositoryElement element : elements) {
				final URI location = element.getLocation();
				if (element.isEnabled()) {
					if (containsURI(currentlyDisabled, location))
						// It should be enabled and is not currently
						setColocatedRepositoryEnablement(ui, location, true);
					else if (!containsURI(currentlyEnabled, location)) {
						// It is not known as enabled or disabled.  Add it.
						metaManager.addRepository(location);
						artManager.addRepository(location);
					}
				} else {
					if (containsURI(currentlyEnabled, location))
						// It should be disabled, and is currently enabled
						setColocatedRepositoryEnablement(ui, location, false);
					else if (!containsURI(currentlyDisabled, location)) {
						// It is not known as enabled or disabled.  Add it and then disable it.
						metaManager.addRepository(location);
						artManager.addRepository(location);
						setColocatedRepositoryEnablement(ui, location, false);
					}
				}
				final String name = element.getName();
				if (name != null && name.length() > 0) {
					metaManager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
					artManager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
				}
			}
			// Are there any elements that need to be deleted?  Go over the original state
			// and remove any elements that weren't in the elements we were given
			final Set<String> nowKnown = new HashSet<>();
			for (final MetadataRepositoryElement element : elements)
				nowKnown.add(URIUtil.toUnencodedString(element.getLocation()));
			for (final URI element : currentlyEnabled) {
				if (!nowKnown.contains(URIUtil.toUnencodedString(element))) {
					metaManager.removeRepository(element);
					artManager.removeRepository(element);
				}
			}
			for (final URI element : currentlyDisabled) {
				if (!nowKnown.contains(URIUtil.toUnencodedString(element))) {
					metaManager.removeRepository(element);
					artManager.removeRepository(element);
				}
			}
		} finally {
			ui.signalRepositoryOperationComplete(null, true);
		}
	}

	private static void setColocatedRepositoryEnablement(final ProvisioningUI ui, final URI location, final boolean enable) {
		ProvUI.getArtifactRepositoryManager(ui.getSession()).setEnabled(location, enable);
		ProvUI.getMetadataRepositoryManager(ui.getSession()).setEnabled(location, enable);
	}

	public static IInstallableUnit getIU(final Object element) {
		if (element instanceof IInstallableUnit)
			return (IInstallableUnit) element;
		if (element instanceof IIUElement)
			return ((IIUElement) element).getIU();
		return ProvUI.getAdapter(element, IInstallableUnit.class);
	}

	public static List<IInstallableUnit> elementsToIUs(final Object[] elements) {
		final ArrayList<IInstallableUnit> theIUs = new ArrayList<>(elements.length);
		for (final Object element : elements) {
			final IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
			if (iu != null)
				theIUs.add(iu);
		}
		return theIUs;
	}

	public static IInstallableUnit elementToIU(final Object selectedElement) {
		return ProvUI.getAdapter(selectedElement, IInstallableUnit.class);
	}

	static boolean containsURI(final URI[] locations, final URI url) {
		for (final URI location : locations)
			if (location.equals(url))
				return true;
		return false;
	}

	public static AvailableIUElement[] requestToElement(final IEclipseContext ctx, final Remedy remedy, final boolean installMode) {
		if (remedy == null)
			return new AvailableIUElement[0];
		final ArrayList<AvailableIUElement> temp = new ArrayList<>();
		final ProvisioningUI ui = ProvisioningUI.getDefaultUI(ctx);
		final IUElementListRoot root = new IUElementListRoot(ctx, ui);
		for (final RemedyIUDetail iuDetail : remedy.getIusDetails()) {
			if (iuDetail.getStatus() == RemedyIUDetail.STATUS_NOT_ADDED)
				continue;
			final AvailableIUElement element = new AvailableIUElement(ctx, root, iuDetail.getIu(), ui.getProfileId(), true);
			if (iuDetail.getBeingInstalledVersion() != null && iuDetail.getRequestedVersion() != null && iuDetail.getBeingInstalledVersion().compareTo(iuDetail.getRequestedVersion()) < 0 && !installMode)
				element.setImageOverlayId(ProvUIImages.IMG_INFO);
			else if (iuDetail.getStatus() == RemedyIUDetail.STATUS_REMOVED)
				element.setImageId(ProvUIImages.IMG_REMOVED);
			temp.add(element);
		}
		return temp.toArray(new AvailableIUElement[temp.size()]);
	}

	public static RemedyElementCategory[] requestToRemedyElementsCategories(final Remedy remedy) {
		final List<RemedyElementCategory> categories = new ArrayList<>();
		final RemedyElementCategory categoryAdded = new RemedyElementCategory(ProvUIMessages.RemedyCategoryAdded);
		final RemedyElementCategory cateogyRemoved = new RemedyElementCategory(ProvUIMessages.RemedyCategoryRemoved);
		final RemedyElementCategory categoryNotAdded = new RemedyElementCategory(ProvUIMessages.RemedyCategoryNotAdded);
		final RemedyElementCategory categoryChanged = new RemedyElementCategory(ProvUIMessages.RemedyCategoryChanged);
		for (final RemedyIUDetail remedyIUVersions : remedy.getIusDetails()) {
			if (remedyIUVersions.getStatus() == RemedyIUDetail.STATUS_ADDED)
				categoryAdded.add(remedyIUVersions);
			else if (remedyIUVersions.getStatus() == RemedyIUDetail.STATUS_CHANGED)
				categoryChanged.add(remedyIUVersions);
			else if (remedyIUVersions.getStatus() == RemedyIUDetail.STATUS_REMOVED)
				cateogyRemoved.add(remedyIUVersions);
			else if (remedyIUVersions.getStatus() == RemedyIUDetail.STATUS_NOT_ADDED)
				categoryNotAdded.add(remedyIUVersions);
		}

		if (cateogyRemoved.getElements().size() > 0)
			categories.add(cateogyRemoved);
		if (categoryChanged.getElements().size() > 0)
			categories.add(categoryChanged);
		if (categoryNotAdded.getElements().size() > 0)
			categories.add(categoryNotAdded);
		if (categoryAdded.getElements().size() > 0)
			categories.add(categoryAdded);
		return categories.toArray(new RemedyElementCategory[categories.size()]);
	}
}
