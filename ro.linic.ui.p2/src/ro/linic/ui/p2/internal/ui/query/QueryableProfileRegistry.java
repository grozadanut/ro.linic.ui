package ro.linic.ui.p2.internal.ui.query;

import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An object that adds queryable support to the profile registry.
 */
public class QueryableProfileRegistry implements IQueryable<IProfile> {

	private ProvisioningUI ui;

	public QueryableProfileRegistry(final ProvisioningUI ui) {
		this.ui = ui;
	}

	@Override
	public IQueryResult<IProfile> query(final IQuery<IProfile> query, final IProgressMonitor monitor) {
		final IProfile[] profiles = ProvUI.getProfileRegistry(ui.getSession()).getProfiles();
		final SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableProfileRegistry_QueryProfileProgress, profiles.length);
		try {
			return query.perform(Arrays.asList(profiles).iterator());
		} finally {
			sub.done();
		}
	}
}
