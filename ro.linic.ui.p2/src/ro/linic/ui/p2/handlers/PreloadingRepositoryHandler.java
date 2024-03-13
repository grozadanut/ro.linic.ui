package ro.linic.ui.p2.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * PreloadingRepositoryHandler provides background loading of
 * repositories before executing the provisioning handler.
 *
 * @since 3.5
 */
abstract class PreloadingRepositoryHandler {

	@Execute
	public void execute(final IEclipseContext ctx, final Shell shell, final UISynchronize sync) {
		// Look for a profile.  We may not immediately need it in the
		// handler, but if we don't have one, whatever we are trying to do
		// will ultimately fail in a more subtle/low-level way.  So determine
		// up front if the system is configured properly.
		final String profileId = getProvisioningUI(ctx).getProfileId();
		final IProvisioningAgent agent = getProvisioningUI(ctx).getSession().getProvisioningAgent();
		IProfile profile = null;
		if (agent != null) {
			final IProfileRegistry registry = agent.getService(IProfileRegistry.class);
			if (registry != null) {
				profile = registry.getProfile(profileId);
			}
		}
		if (profile == null) {
			// Inform the user nicely
			MessageDialog.openInformation(null, ProvUIMessages.Handler_SDKUpdateUIMessageTitle, ProvUIMessages.Handler_CannotLaunchUI);
			// Log the detailed message
			ProvUI.reportStatus(ctx, new Status(IStatus.WARNING, ProvUIAddon.PLUGIN_ID, ProvUIMessages.ProvSDKUIActivator_NoSelfProfile),
					StatusManager.LOG);
		} else {
			try {
				final ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
				dialog.run(true, true, monitor -> refreshRepositories(ctx, monitor));
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
			BusyIndicator.showWhile(shell.getDisplay(), () -> doExecuteAndLoad(ctx, shell, sync));
		}
	}

	void doExecuteAndLoad(final IEclipseContext ctx, final Shell shell, final UISynchronize sync) {
		if (preloadRepositories(ctx)) {
			//cancel any load that is already running
			Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
			final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(ctx, getProvisioningUI(ctx)) {

				@Override
				public IStatus runModal(final IProgressMonitor monitor) {
					final SubMonitor sub = SubMonitor.convert(monitor, getProgressTaskName(), 1000);
					final IStatus status = super.runModal(sub.newChild(500));
					if (status.getSeverity() == IStatus.CANCEL)
						return status;
					try {
						doPostLoadBackgroundWork(ctx, sub.newChild(500));
					} catch (final OperationCanceledException e) {
						return Status.CANCEL_STATUS;
					}
					if (shouldAccumulateFailures()) {
						// If we are accumulating failures, don't return a combined status here. By returning OK,
						// we are indicating that the operation should continue with the repositories that
						// did load.
						return Status.OK_STATUS;
					}
					return status;
				}
			};
			setLoadJobProperties(ctx, loadJob);
			if (waitForPreload(ctx)) {
				loadJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(final IJobChangeEvent event) {
						if (!shell.isDisposed())
							if (event.getResult().isOK()) {
								sync.asyncExec(() -> doExecute(ctx, shell, sync, loadJob));
							}
					}
				});
				loadJob.setUser(true);
				loadJob.schedule();

			} else {
				loadJob.setSystem(true);
				loadJob.setUser(false);
				loadJob.schedule();
				doExecute(ctx, shell, sync, null);
			}
		} else {
			doExecute(ctx, shell, sync, null);
		}
	}

	protected abstract String getProgressTaskName();

	protected abstract void doExecute(IEclipseContext ctx, final Shell shell, final UISynchronize sync, LoadMetadataRepositoryJob job);

	protected boolean preloadRepositories(final IEclipseContext ctx) {
		return true;
	}

	protected void doPostLoadBackgroundWork(final IEclipseContext ctx, final IProgressMonitor monitor) throws OperationCanceledException {
		// default is to do nothing more.
	}

	protected boolean waitForPreload(final IEclipseContext ctx) {
		return true;
	}

	protected void setLoadJobProperties(final IEclipseContext ctx, final Job loadJob) {
		loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));
	}

	protected ProvisioningUI getProvisioningUI(final IEclipseContext ctx) {
		return ProvisioningUI.getDefaultUI(ctx);
	}
	
	private void refreshRepositories(final IEclipseContext ctx, final IProgressMonitor monitor) {
		final ProvisioningUI ui = getProvisioningUI(ctx);
		ui.getRepositoryTracker().refreshRepositories(ui.getRepositoryTracker().getKnownRepositories(ui.getSession()),
				ui.getSession(), monitor);
	}
}
