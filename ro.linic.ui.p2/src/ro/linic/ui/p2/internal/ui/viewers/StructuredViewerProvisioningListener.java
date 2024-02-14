package ro.linic.ui.p2.internal.ui.viewers;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;

import ro.linic.ui.p2.internal.ui.ProvUIProvisioningListener;
import ro.linic.ui.p2.internal.ui.ProvisioningOperationRunner;
import ro.linic.ui.p2.internal.ui.model.ProfileElement;

/**
 * ProvisioningListener which updates a structured viewer based on
 * provisioning changes.  Provides default behavior which refreshes particular
 * model elements or the entire viewer based on the nature of the change and the
 * changes that the client is interested in.  Subclasses typically only need
 * override when there is additional, specialized behavior required.
 *
 * @since 3.4
 */
public class StructuredViewerProvisioningListener extends ProvUIProvisioningListener {

	StructuredViewer viewer;
	Display display;
	private IEclipseContext ctx;

	public StructuredViewerProvisioningListener(final IEclipseContext ctx, final String name, final StructuredViewer viewer,
			final int eventTypes, final ProvisioningOperationRunner runner) {
		super(name, eventTypes, runner);
		this.ctx = ctx;
		this.viewer = viewer;
		this.display = viewer.getControl().getDisplay();
	}

	/**
	 * A repository has been added.  The default behavior is to
	 * refresh the viewer.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param event the RepositoryEvent describing the details
	 */
	@Override
	protected void repositoryAdded(final RepositoryEvent event) {
		safeRefresh();
	}

	/**
	 * A repository has been removed.  The default behavior is to
	 * refresh the viewer.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param event the RepositoryEvent describing the details
	 */
	@Override
	protected void repositoryRemoved(final RepositoryEvent event) {
		safeRefresh();
	}

	/**
	 * A repository has been discovered.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param event the RepositoryEvent describing the details
	 */
	@Override
	protected void repositoryDiscovered(final RepositoryEvent event) {
		// Do nothing for now
	}

	/**
	 * A repository has changed.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param event the RepositoryEvent describing the details
	 */
	@Override
	protected void repositoryChanged(final RepositoryEvent event) {
		// Do nothing for now.  When this event is actually used in
		// the core, we may want to refresh particular elements the way
		// we currently refresh a profile element.
	}

	/**
	 * The specified profile has changed.  The default behavior is to refresh the viewer
	 * with a profile element of the matching id.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param profileId the id of the profile that changed.
	 */
	@Override
	protected void profileChanged(final String profileId) {
		display.asyncExec(() -> {
			if (isClosing())
				return;
			// We want to refresh the affected profile, so we
			// construct a profile element on this profile.
			final ProfileElement element = new ProfileElement(ctx, null, profileId);
			viewer.refresh(element);
		});
	}

	/**
	 * The specified profile has been added.  The default behavior is to fully
	 * refresh the associated viewer. Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param profileId the id of the profile that has been added.
	 */
	@Override
	protected void profileAdded(final String profileId) {
		safeRefresh();
	}

	/**
	 * The specified profile has been removed.  The default behavior is to fully
	 * refresh the associated viewer. Subclasses may override.  May be called
	 * from a non-UI thread.
	 *
	 * @param profileId the id of the profile that has been removed.
	 */
	@Override
	protected void profileRemoved(final String profileId) {
		safeRefresh();
	}

	protected void safeRefresh() {
		if (Display.getCurrent() != null) {
			refreshViewer();
			return;
		}

		display.asyncExec(() -> {
			if (isClosing())
				return;
			refreshViewer();
		});
	}

	@Override
	protected void refreshAll() {
		safeRefresh();
	}

	/**
	 * Refresh the entire structure of the viewer.  Subclasses may
	 * override to ensure that any caching done in content providers or
	 * model elements is refreshed before the viewer is refreshed.  This will
	 * always be called from the UI thread.
	 */
	protected void refreshViewer() {
		viewer.refresh();
	}

	/**
	 * Return whether the viewer is closing or shutting down.
	 * This method should be used in async execs to ensure that
	 * the viewer is still alive.
	 * @return a boolean indicating whether the viewer is closing
	 */
	protected boolean isClosing() {
		if (Display.getCurrent() == null)
			return true;

		if (viewer.getControl().isDisposed())
			return true;

		return false;
	}
}
