package ro.linic.ui.p2.internal.ui.query;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.AvailableIUElement;
import ro.linic.ui.p2.internal.ui.model.CategoryElement;
import ro.linic.ui.p2.internal.ui.model.IIUElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElementWrapper;

/**
 * A wrapper that examines available IU's and wraps them in an
 * element representing either a category or a regular IU.
 *
 * @since 3.4
 */
public class AvailableIUWrapper extends QueriedElementWrapper {

	private boolean makeCategories;
	private IProfile profile;
	private boolean hideInstalledIUs = false;
	private boolean drillDownChild = false;

	public AvailableIUWrapper(final IEclipseContext ctx, final IQueryable<?> queryable, final Object parent, final boolean makeCategories,
			final boolean makeDrillDownChild) {
		super(ctx, queryable, parent);
		this.makeCategories = makeCategories;
		this.drillDownChild = makeDrillDownChild;
	}

	public void markInstalledIUs(final IProfile targetProfile, final boolean hideInstalled) {
		this.profile = targetProfile;
		hideInstalledIUs = hideInstalled;
	}

	class InformationCache {
		Object item = null;
		boolean isUpdate = false;
		boolean isInstalled = false;
		boolean isPatch = false;

		public InformationCache(final Object item, final boolean isUpdate, final boolean isInstalled, final boolean isPatch) {
			this.item = item;
			this.isUpdate = isUpdate;
			this.isInstalled = isInstalled;
			this.isPatch = isPatch;
		}
	}

	InformationCache cache = null;

	@Override
	protected boolean shouldWrap(final Object match) {
		final IInstallableUnit iu = ProvUI.getAdapter(match, IInstallableUnit.class);
		cache = computeIUInformation(iu); // Cache the result

		// if we are hiding, hide anything that is the same iu or older
		if (hideInstalledIUs && cache.isInstalled && !cache.isUpdate) {
			emptyExplanationString = ProvUIMessages.AvailableIUWrapper_AllAreInstalled;
			emptyExplanationSeverity = IStatus.INFO;
			emptyExplanationDescription = ProvUIMessages.IUViewQueryContext_AllAreInstalledDescription;
			return false;
		}
		return true;
	}

	/**
	 * Compute information about this IU. This computes whether or
	 * not this IU is installed and / or updated.
	 */
	private InformationCache computeIUInformation(final IInstallableUnit iu) {
		boolean isUpdate = false;
		boolean isInstalled = false;
		final boolean isPatch = iu == null ? false : QueryUtil.isPatch(iu);
		if (profile != null && iu != null) {
			isInstalled = !profile.query(QueryUtil.createIUQuery(iu), null).isEmpty();
			final Iterator<IInstallableUnit> iter = profile.query(new UserVisibleRootQuery(), null).iterator();
			while (iter.hasNext()) {
				final IInstallableUnit installed = iter.next();
				if (iu.getUpdateDescriptor() != null && iu.getUpdateDescriptor().isUpdateOf(installed) && (!iu.getId().equals(installed.getId()) || installed.getVersion().compareTo(iu.getVersion()) < 0)) {
					isUpdate = true;
					break;
				}
			}
		}
		return new InformationCache(iu, isUpdate, isInstalled, isPatch);
	}

	@Override
	protected Object wrap(final Object item) {
		final IInstallableUnit iu = ProvUI.getAdapter(item, IInstallableUnit.class);
		boolean isUpdate = false;
		boolean isInstalled = false;
		boolean isPatch = false;
		if (cache != null && cache.item == item) {
			// This cache should always be valide, since accept is called before transformItem
			isUpdate = cache.isUpdate;
			isInstalled = cache.isInstalled;
			isPatch = cache.isPatch;
		} else {
			final InformationCache iuInformation = computeIUInformation(iu);
			isUpdate = iuInformation.isUpdate;
			isInstalled = iuInformation.isInstalled;
			isPatch = iuInformation.isPatch;
		}
		// subclass already made this an element, just set the install flag
		if (item instanceof AvailableIUElement) {
			final AvailableIUElement element = (AvailableIUElement) item;
			element.setIsInstalled(isInstalled);
			element.setIsUpdate(isUpdate);
			element.setIsPatch(isPatch);
			return super.wrap(item);
		}
		// If it's not an IU or element, we have nothing to do here
		if (!(item instanceof IInstallableUnit))
			return super.wrap(item);

		// We need to make an element
		if (makeCategories && isCategory(iu))
			return super.wrap(new CategoryElement(ctx, parent, iu));

		final IIUElement element = makeDefaultElement(iu);
		if (element instanceof AvailableIUElement) {
			final AvailableIUElement availableElement = (AvailableIUElement) element;
			availableElement.setIsInstalled(isInstalled);
			availableElement.setIsUpdate(isUpdate);
			availableElement.setIsPatch(isPatch);
		}
		return super.wrap(element);
	}

	protected IIUElement makeDefaultElement(final IInstallableUnit iu) {
		if (parent instanceof AvailableIUElement)
			drillDownChild = ((AvailableIUElement) parent).shouldShowChildren();
		return new AvailableIUElement(ctx, parent, iu, null, drillDownChild);
	}

	protected boolean isCategory(final IInstallableUnit iu) {
		return QueryUtil.isCategory(iu);
	}

	protected boolean makeCategory() {
		return makeCategories;
	}
}
