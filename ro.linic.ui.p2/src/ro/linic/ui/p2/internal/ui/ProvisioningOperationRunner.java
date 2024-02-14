package ro.linic.ui.p2.internal.ui;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.operations.ProvisioningJob;

import ro.linic.ui.p2.internal.ui.dialogs.ApplyProfileChangesDialog;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Utility methods for running provisioning operations.   Operations can either
 * be run synchronously or in a job.  When scheduled as a job, the operation
 * determines whether the job is run in
 * the background or in the UI.
 *
 * @since 3.4
 */
public class ProvisioningOperationRunner {

	boolean suppressRestart = false;
	ProvisioningUI ui;
	public int eventBatchCount = 0;
	
	private IEclipseContext ctx;

	public ProvisioningOperationRunner(final IEclipseContext ctx, final ProvisioningUI ui) {
		this.ctx = ctx;
		this.ui = ui;
	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation.
	 *
	 * @param job The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public void schedule(final ProvisioningJob job, final int errorStyle) {
		final boolean noPrompt = (errorStyle & (StatusManager.BLOCK | StatusManager.SHOW)) == 0;
		if (noPrompt) {
//			job.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
//			job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
		}
//		job.setProperty(IProgressConstants.ICON_PROPERTY, ProvUIImages.getImageDescriptor(ctx, ProvUIImages.IMG_PROFILE));
//		job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
		manageJob(job, job.getRestartPolicy());
		job.schedule();
	}

	/**
	 * Request a restart of the platform according to the specified
	 * restart policy.
	 *
	 * @param restartPolicy
	 */
	void requestRestart(final int restartPolicy) {
		final UISynchronize sync = ctx.get(UISynchronize.class);
		final IWorkbench workbench = ctx.get(IWorkbench.class);
		// Global override of restart (used in test cases).
		if (suppressRestart)
			return;
		if (restartPolicy == Policy.RESTART_POLICY_FORCE) {
			workbench.restart();
			return;
		}
		if (restartPolicy == Policy.RESTART_POLICY_FORCE_APPLY) {
			applyProfileChanges();
			return;
		}

		sync.asyncExec(() -> {
//			if (PlatformUI.getWorkbench().isClosing())
//				return;
			final int retCode = ApplyProfileChangesDialog.promptForRestart(ProvUI.getDefaultParentShell(), restartPolicy == Policy.RESTART_POLICY_PROMPT);
			if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
				applyProfileChanges();
			} else if (retCode == ApplyProfileChangesDialog.PROFILE_RESTART) {
				workbench.restart();
			}
		});
	}

	void applyProfileChanges() {
		final Configurator configurator = ctx.get(Configurator.class);
		try {
			configurator.applyConfiguration();
		} catch (final IOException e) {
			ProvUI.handleException(e, ProvUIMessages.ProvUI_ErrorDuringApplyConfig, StatusManager.LOG | StatusManager.BLOCK);
		} catch (final IllegalStateException e) {
			final IStatus illegalApplyStatus = new Status(IStatus.WARNING, ProvUIAddon.PLUGIN_ID, 0, ProvUIMessages.ProvisioningOperationRunner_CannotApplyChanges, e);
			ProvUI.reportStatus(ctx, illegalApplyStatus, StatusManager.LOG | StatusManager.BLOCK);
		}
	}

	public void manageJob(final Job job, final int jobRestartPolicy) {
		ui.getSession().rememberJob(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				final int severity = event.getResult().getSeverity();
				// If the job finished without error, see if restart is needed
				if (severity != IStatus.CANCEL && severity != IStatus.ERROR) {
					if (jobRestartPolicy == ProvisioningJob.RESTART_NONE) {
						return;
					}
					final int globalRestartPolicy = ui.getPolicy().getRestartPolicy();
					// If the global policy allows apply changes, check the job policy to see if it supports it.
					if (globalRestartPolicy == Policy.RESTART_POLICY_PROMPT_RESTART_OR_APPLY) {
						if (jobRestartPolicy == ProvisioningJob.RESTART_OR_APPLY)
							requestRestart(Policy.RESTART_POLICY_PROMPT_RESTART_OR_APPLY);
						else
							requestRestart(Policy.RESTART_POLICY_PROMPT);
					} else
						requestRestart(globalRestartPolicy);
				}
			}
		});
	}

	/**
	 * This method is provided for use in automated test case.  It should
	 * no longer be needed to be used by clients.
	 *
	 * @param suppress <code>true</code> to suppress all restarts and <code>false</code>
	 * to stop suppressing restarts.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void suppressRestart(final boolean suppress) {
		suppressRestart = suppress;
	}
}
