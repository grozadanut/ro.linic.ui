package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

import ro.linic.ui.p2.internal.ui.ProvUIImages;

/**
 * Element wrapper class for IU's that are available for installation. Used
 * instead of the plain IU when additional information such as sizing info is
 * necessary.
 *
 * @since 3.4
 */
public class AvailableUpdateElement extends AvailableIUElement {

	IInstallableUnit iuToBeUpdated;
	boolean isLockedForUpdate = false;

	public AvailableUpdateElement(final IEclipseContext ctx, final Object parent, final IInstallableUnit iu, final IInstallableUnit iuToBeUpdated,
			final String profileID, final boolean shouldShowChildren) {
		super(ctx, parent, iu, profileID, shouldShowChildren);
		setIsInstalled(false);
		this.iuToBeUpdated = iuToBeUpdated;
		init();

	}

	private void init() {
		final IProfileRegistry profileRegistry = getProvisioningUI().getSession().getProvisioningAgent()
				.getService(IProfileRegistry.class);
		final IProfile profile = profileRegistry.getProfile(profileID);
		final String property = profile.getInstallableUnitProperty(iuToBeUpdated, IProfile.PROP_PROFILE_LOCKED_IU);
		try {
			isLockedForUpdate = property == null ? false : (Integer.parseInt(property) & IProfile.LOCK_UPDATE) > 0;
		} catch (final NumberFormatException e) {
			isLockedForUpdate = false;
		}
	}

	public boolean isLockedForUpdate() {
		return isLockedForUpdate;
	}

	public IInstallableUnit getIUToBeUpdated() {
		return iuToBeUpdated;
	}

	@Override
	protected IProvisioningPlan getSizingPlan(final IProgressMonitor monitor) {
		final IPlanner planner = getPlanner();
		final IProfileChangeRequest request = ProfileChangeRequest
				.createByProfileId(getProvisioningUI().getSession().getProvisioningAgent(), profileID);
		if (iuToBeUpdated.getId().equals(getIU().getId()))
			request.remove(iuToBeUpdated);
		request.add(getIU());
		return planner.getProvisioningPlan(request,
				new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent()), monitor);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AvailableUpdateElement))
			return false;
		if (iu == null)
			return false;
		if (iuToBeUpdated == null)
			return false;
		final AvailableUpdateElement other = (AvailableUpdateElement) obj;
		return iu.equals(other.getIU()) && iuToBeUpdated.equals(other.getIUToBeUpdated());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((iu == null) ? 0 : iu.hashCode());
		result = prime * result + ((iuToBeUpdated == null) ? 0 : iuToBeUpdated.hashCode());
		return result;
	}

	public Update getUpdate() {
		return new Update(iuToBeUpdated, getIU());
	}

	@Override
	protected String getImageId(final Object obj) {
		final String imageId = super.getImageId(obj);
		if (ProvUIImages.IMG_IU.equals(imageId) && isLockedForUpdate())
			return ProvUIImages.IMG_DISABLED_IU;
		return imageId;
	}
}
