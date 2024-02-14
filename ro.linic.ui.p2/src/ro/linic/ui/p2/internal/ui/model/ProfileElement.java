package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * Element wrapper class for a profile that uses the query mechanism to obtain
 * its contents.
 *
 * @since 3.4
 */
public class ProfileElement extends RemoteQueriedElement {
	String profileId;

	public ProfileElement(final IEclipseContext ctx, final Object parent, final String profileId) {
		super(ctx, parent);
		this.profileId = profileId;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IProfile.class)
			return (T) getQueryable();
		return super.getAdapter(adapter);
	}

	@Override
	protected String getImageId(final Object obj) {
		return ProvUIImages.IMG_PROFILE;
	}

	@Override
	public String getLabel(final Object o) {
		return profileId;
	}

	public String getProfileId() {
		return profileId;
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	@Override
	public IQueryable<?> getQueryable() {
		return ProvUI.getProfileRegistry(getProvisioningUI().getSession()).getProfile(profileId);
	}

	/*
	 * Overridden to check whether we know the profile id rather than fetch the
	 * profile from the registry using getQueryable()
	 */
	@Override
	public boolean knowsQueryable() {
		return profileId != null;
	}

	/*
	 * Overridden to check the children so that profiles showing in profile views
	 * accurately reflect if they are empty. We do not cache the children because
	 * often this element is the input of a view and when the view is refreshed we
	 * want to refetch the children.
	 */
	@Override
	public boolean isContainer() {
		return super.getChildren(this).length > 0;
	}
}
