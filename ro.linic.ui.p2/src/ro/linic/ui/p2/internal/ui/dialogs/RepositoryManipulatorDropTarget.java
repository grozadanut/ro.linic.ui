package ro.linic.ui.p2.internal.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.progress.UIJob;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Control;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * RepositoryManipulatorDropTarget recognizes both URLTransfer and
 * FileTransfer data types.  Files are converted to URL's with the file
 * protocol.  Any dropped URLs (or Files) are interpreted to mean that the
 * user wishes to add these files as repositories.
 *
 * @since 3.4
 *
 */
public class RepositoryManipulatorDropTarget extends URLDropAdapter {
	ProvisioningUI ui;
	RepositoryTracker tracker;
	Control control;
	private IEclipseContext ctx;

	public RepositoryManipulatorDropTarget(final IEclipseContext ctx, final ProvisioningUI ui, final Control control) {
		super(true); // convert file drops to URL
		Assert.isNotNull(ui);
		this.ctx = ctx;
		this.ui = ui;
		this.tracker = ui.getRepositoryTracker();
		this.control = control;
	}

	@Override
	protected void handleDrop(final String urlText, final DropTargetEvent event) {
		event.detail = DND.DROP_NONE;
		final URI[] location = new URI[1];
		try {
			location[0] = URIUtil.fromString(urlText);
		} catch (final URISyntaxException e) {
			ProvUI.reportStatus(ctx, tracker.getInvalidLocationStatus(urlText), StatusManager.SHOW | StatusManager.LOG);
			return;
		}
		if (location[0] == null)
			return;

		final Job job = UIJob.create(ProvUIMessages.RepositoryManipulatorDropTarget_DragAndDropJobLabel, monitor -> {
			IStatus status = tracker.validateRepositoryLocation(ui.getSession(), location[0], false, monitor);
			if (status.isOK()) {
				tracker.addRepository(location[0], null, ui.getSession());
				event.detail = DND.DROP_LINK;
			} else if (status.getSeverity() == IStatus.CANCEL) {
				event.detail = DND.DROP_NONE;
			} else {
				status = new MultiStatus(ProvUIAddon.PLUGIN_ID, 0, new IStatus[] { status },
						NLS.bind(ProvUIMessages.RepositoryManipulatorDropTarget_DragSourceNotValid,
								URIUtil.toUnencodedString(location[0])),
						null);
				event.detail = DND.DROP_NONE;
			}
			return status;
		});
		job.setPriority(Job.SHORT);
		job.setUser(true);
		job.schedule();
	}
}