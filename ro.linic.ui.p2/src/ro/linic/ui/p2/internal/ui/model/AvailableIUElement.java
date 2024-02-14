package ro.linic.ui.p2.internal.ui.model;

import java.net.URI;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.repository.IRepository;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * Element wrapper class for IU's that are available for installation. Used
 * instead of the plain IU when additional information such as sizing info is
 * necessary.
 *
 * @since 3.4
 */
public class AvailableIUElement extends QueriedElement implements IIUElement {

	IInstallableUnit iu;
	boolean shouldShowChildren;
	boolean isInstalled = false;
	boolean isUpdate = false;
	boolean isPatch = false;
	boolean beingAdded = false;
	boolean beingDowngraded = false;
	boolean beingUpgraded = false;
	boolean beingRemoved = false;
	private String imageId;
	private String imageOverlayId;

	// Currently this variable is not settable due to the
	// poor performance of sizing, but it is kept here for future improvement.
	// If we reinstate the ability to compute individual sizes we would
	// probably refer to some preference or policy to decide what to do.
	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=221087
	private static boolean shouldShowSize = false;
	long size = ProvUI.SIZE_UNKNOWN;
	String profileID;

	public AvailableIUElement(final IEclipseContext ctx, final Object parent, final IInstallableUnit iu, final String profileID, final boolean showChildren) {
		super(ctx, parent);
		this.iu = iu;
		this.profileID = profileID;
		this.shouldShowChildren = showChildren;
		this.isPatch = iu == null ? false : Boolean.valueOf(iu.getProperty(InstallableUnitDescription.PROP_TYPE_PATCH));
	}

	@Override
	protected String getImageId(final Object obj) {
		if (imageId != null)
			return imageId;
		if (isUpdate)
			return ProvUIImages.IMG_UPDATED_IU;
		else if (isPatch)
			return isInstalled ? ProvUIImages.IMG_DISABLED_PATCH_IU : ProvUIImages.IMG_PATCH_IU;
		else if (isInstalled)
			return ProvUIImages.IMG_DISABLED_IU;
		if (beingDowngraded)
			return ProvUIImages.IMG_DOWNGRADED_IU;
		if (beingUpgraded)
			return ProvUIImages.IMG_UPGRADED_IU;
		return ProvUIImages.IMG_IU;
	}

	@Override
	public String getImageOverlayId(final Object obj) {
		if (imageOverlayId != null)
			return imageOverlayId;
		if (beingRemoved)
			return ProvUIImages.IMG_REMOVED_OVERLAY;
		if (beingAdded)
			return ProvUIImages.IMG_ADDED_OVERLAY;
		return null;
	}

	@Override
	public String getLabel(final Object o) {
		return iu.getId();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IInstallableUnit.class)
			return (T) iu;
		return super.getAdapter(adapter);
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public void computeSize(final IProgressMonitor monitor) {
		if (profileID == null)
			return;
		final SubMonitor mon = SubMonitor.convert(monitor, 100);
		final IProvisioningPlan plan = getSizingPlan(mon.newChild(50));
		size = ProvUI.getSize(getEngine(), plan, getProvisioningContext(), mon.newChild(50));
	}

	protected IProfile getProfile() {
		return getProfileRegistry().getProfile(profileID);
	}

	protected IProvisioningPlan getSizingPlan(final IProgressMonitor monitor) {
		final IPlanner planner = getPlanner();
		final IProfileChangeRequest request = ProfileChangeRequest
				.createByProfileId(getProvisioningUI().getSession().getProvisioningAgent(), profileID);
		request.add(getIU());
		return planner.getProvisioningPlan(request, getProvisioningContext(), monitor);
	}

	IEngine getEngine() {
		return ProvUI.getEngine(getProvisioningUI().getSession());
	}

	IPlanner getPlanner() {
		return getProvisioningUI().getSession().getProvisioningAgent().getService(IPlanner.class);
	}

	IProfileRegistry getProfileRegistry() {
		return ProvUI.getProfileRegistry(getProvisioningUI().getSession());
	}

	@Override
	public IInstallableUnit getIU() {
		return iu;
	}

	@Override
	public boolean shouldShowSize() {
		return shouldShowSize;
	}

	@Override
	public boolean shouldShowVersion() {
		return true;
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		return iu.getRequirements();
	}

	@Override
	public boolean shouldShowChildren() {
		return shouldShowChildren;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AvailableIUElement))
			return false;
		if (iu == null)
			return false;
		if (!iu.equals(((AvailableIUElement) obj).getIU()))
			return false;

		final Object parent = getParent(this);
		final Object objParent = ((AvailableIUElement) obj).getParent(obj);
		if (parent != null && objParent != null)
			return parent.equals(objParent);
		else if (parent == null && objParent == null)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		if (iu == null)
			return 0;
		return iu.hashCode();
	}

	@Override
	public String toString() {
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		return iu.toString();
	}

	public void setIsInstalled(final boolean isInstalled) {
		this.isInstalled = isInstalled;
	}

	public boolean isInstalled() {
		return isInstalled;
	}

	public void setIsUpdate(final boolean isUpdate) {
		this.isUpdate = isUpdate;
	}

	public boolean isUpdate() {
		return isUpdate;
	}

	public void setIsPatch(final boolean isPatch) {
		this.isPatch = isPatch;
	}

	public boolean isPatch() {
		return isPatch;
	}

	private ProvisioningContext getProvisioningContext() {
		final ProvisioningContext context = new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent());
		if (hasQueryable() && getQueryable() instanceof IRepository<?>) {
			context.setMetadataRepositories(new URI[] { ((IRepository<?>) getQueryable()).getLocation() });
		}
		return context;
	}

	public boolean isBeingAdded() {
		return beingAdded;
	}

	public void setBeingAdded(final boolean beingAdded) {
		this.beingAdded = beingAdded;
	}

	public boolean isBeingDowngraded() {
		return beingDowngraded;
	}

	public void setBeingDowngraded(final boolean beingDowngraded) {
		this.beingDowngraded = beingDowngraded;
	}

	public boolean isBeingUpgraded() {
		return beingUpgraded;
	}

	public void setBeingUpgraded(final boolean beingUpgraded) {
		this.beingUpgraded = beingUpgraded;
	}

	public boolean isBeingRemoved() {
		return beingRemoved;
	}

	public void setBeingRemoved(final boolean beingRemoved) {
		this.beingRemoved = beingRemoved;
	}

	public void setImageId(final String imageId) {
		this.imageId = imageId;
	}

	public void setImageOverlayId(final String imageOverlayId) {
		this.imageOverlayId = imageOverlayId;
	}
}
