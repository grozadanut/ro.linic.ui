package ro.linic.ui.p2.internal.ui.query;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.model.InstalledIUElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElementWrapper;

/**
 * ElementWrapper that accepts the matched IU's and
 * wraps them in an InstalledIUElement.
 *
 * @since 3.4
 */
public class InstalledIUElementWrapper extends QueriedElementWrapper {

	public InstalledIUElementWrapper(final IEclipseContext ctx, final IQueryable<?> queryable, final Object parent) {
		super(ctx, queryable, parent);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 *
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	@Override
	protected boolean shouldWrap(final Object match) {
		if (match instanceof IInstallableUnit)
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	@Override
	protected Object wrap(final Object item) {
		if (queryable instanceof IProfile)
			return super.wrap(new InstalledIUElement(ctx, parent, ((IProfile) queryable).getProfileId(), (IInstallableUnit) item));
		// Shouldn't happen, the queryable should typically be a profile
		return super.wrap(item);
	}

}
