
package ro.linic.ui.p2.handlers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.operations.RemedyConfig;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	boolean hasNoRepos = false;
	UpdateOperation operation;

	@Override
	protected void doExecute(final IEclipseContext ctx, final Shell shell, final UISynchronize sync, final LoadMetadataRepositoryJob job) {
		if (hasNoRepos) {
			if (getProvisioningUI(ctx).getPolicy().getRepositoriesVisible()) {
				final boolean goToSites = MessageDialog.openQuestion(shell, ProvUIMessages.UpdateHandler_NoSitesTitle, ProvUIMessages.UpdateHandler_NoSitesMessage);
				if (goToSites) {
					getProvisioningUI(ctx).manipulateRepositories(shell);
				}
			}
			return;
		}
		// Report any missing repositories.
		job.reportAccumulatedStatus();
		if (getProvisioningUI(ctx).getPolicy().continueWorkingWithOperation(operation, shell)) {

			if (operation.getResolutionResult() == Status.OK_STATUS) {
				getProvisioningUI(ctx).openUpdateWizard(false, operation, job);
			} else {

				final RemediationOperation remediationOperation = new RemediationOperation(getProvisioningUI(ctx).getSession(), operation.getProfileChangeRequest(), RemedyConfig.getCheckForUpdateRemedyConfigs());
				final ProvisioningJob job2 = new ProvisioningJob(ProvUIMessages.RemediationOperation_ResolveJobName, getProvisioningUI(ctx).getSession()) {
					@Override
					public IStatus runModal(final IProgressMonitor monitor) {
						monitor.beginTask(ProvUIMessages.RemediationOperation_ResolveJobTask, RemedyConfig.getAllRemedyConfigs().length);
						return remediationOperation.resolveModal(monitor);
					}
				};
				job2.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(final IJobChangeEvent event) {
						if (!shell.isDisposed()) {
							sync.asyncExec(() -> getProvisioningUI(ctx).openUpdateWizard(true, operation, remediationOperation, null));
						}
					}

				});
				getProvisioningUI(ctx).schedule(job2, StatusManager.SHOW | StatusManager.LOG);
			}
		}
	}

	@Override
	protected void doPostLoadBackgroundWork(final IEclipseContext ctx, final IProgressMonitor monitor) throws OperationCanceledException {
		operation = getProvisioningUI(ctx).getUpdateOperation(null, null);
		// check for updates
		final IStatus resolveStatus = operation.resolveModal(monitor);
		if (resolveStatus.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();
	}

	@Override
	protected boolean preloadRepositories(final IEclipseContext ctx) {
		hasNoRepos = false;
		final RepositoryTracker repoMan = getProvisioningUI(ctx).getRepositoryTracker();
		if (repoMan.getKnownRepositories(getProvisioningUI(ctx).getSession()).length == 0) {
			hasNoRepos = true;
			return false;
		}
		return super.preloadRepositories(ctx);
	}

	@Override
	protected String getProgressTaskName() {
		return ProvUIMessages.UpdateHandler_ProgressTaskName;
	}
}
