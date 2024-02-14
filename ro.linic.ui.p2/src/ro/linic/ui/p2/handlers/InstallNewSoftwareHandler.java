package ro.linic.ui.p2.handlers;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;

/**
 * InstallNewSoftwareHandler invokes the install wizard
 * 
 * @since 3.5
 */
public class InstallNewSoftwareHandler extends PreloadingRepositoryHandler {

	@Override
	protected void doExecute(final IEclipseContext ctx, final Shell shell, final UISynchronize sync, final LoadMetadataRepositoryJob job) {
		getProvisioningUI(ctx).openInstallWizard(null, null, job);
	}

	@Override
	protected boolean waitForPreload(final IEclipseContext ctx) {
		// If the user cannot see repositories, then we may as well wait
		// for existing repos to load so that content is available.  
		// If the user can manipulate the repositories, then we don't wait, 
		// because we don't know which ones they want to work with.
		return !getProvisioningUI(ctx).getPolicy().getRepositoriesVisible();
	}

	@Override
	protected void setLoadJobProperties(final IEclipseContext ctx, final Job loadJob) {
		super.setLoadJobProperties(ctx, loadJob);
		// If we are doing a background load, we do not wish to authenticate, as the
		// user is unaware that loading was needed
		if (!waitForPreload(ctx)) {
			loadJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER, Boolean.toString(true));
			loadJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_REPOSITORY_EVENTS, Boolean.toString(true));
		}
	}

	@Override
	protected String getProgressTaskName() {
		return ProvUIMessages.InstallNewSoftwareHandler_ProgressTaskName;
	}
}
