package ro.linic.ui.p2.internal.ui.query;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProfile;

import ro.linic.ui.p2.internal.ui.model.ProfileElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElementWrapper;

/**
 * Collector that accepts the matched Profiles and
 * wraps them in a ProfileElement.
 *
 * @since 3.4
 */
public class ProfileElementWrapper extends QueriedElementWrapper {

	public ProfileElementWrapper(final IEclipseContext ctx, final IProfile profile, final Object parent) {
		super(ctx, profile, parent);
	}

	@Override
	protected boolean shouldWrap(final Object match) {
		if ((match instanceof IProfile))
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	@Override
	protected Object wrap(final Object item) {
		return super.wrap(new ProfileElement(ctx, parent, ((IProfile) item).getProfileId()));
	}

}
