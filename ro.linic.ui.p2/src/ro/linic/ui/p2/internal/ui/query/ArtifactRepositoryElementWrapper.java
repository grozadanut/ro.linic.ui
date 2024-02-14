package ro.linic.ui.p2.internal.ui.query;

import java.net.URI;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.model.ArtifactRepositoryElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElementWrapper;

/**
 * ElementWrapper that wraps a URI with an ArtifactRepositoryElement.
 *
 * @since 3.4
 */
public class ArtifactRepositoryElementWrapper extends QueriedElementWrapper {

	public ArtifactRepositoryElementWrapper(final IEclipseContext ctx, final IQueryable<URI> queryable, final Object parent) {
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
		if ((match instanceof URI))
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	@Override
	protected Object wrap(final Object item) {
		// Assume the item is enabled
		boolean enabled = true;
		// if the parent is a queried element then use its provisioning UI to find out about enablement
		if (parent instanceof QueriedElement) {
			final ProvisioningSession session = ((QueriedElement) parent).getProvisioningUI().getSession();
			enabled = ProvUI.getArtifactRepositoryManager(session).isEnabled((URI) item);
		}
		return super.wrap(new ArtifactRepositoryElement(ctx, parent, (URI) item, enabled));
	}
}
