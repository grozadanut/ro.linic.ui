package ro.linic.ui.base.widgets;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;

import ro.linic.ui.base.Messages;

public class ProgressMonitorControl {

	private final UISynchronize sync;

	private ProgressBar progressBar;

	@Inject
	public ProgressMonitorControl(final UISynchronize sync) {
		this.sync = Objects.requireNonNull(sync);
	}

	@PostConstruct
	public void createControls(final Composite parent) {
		progressBar = new ProgressBar(parent, SWT.SMOOTH);

		Job.getJobManager().setProgressProvider(new ProgressProvider() {

			@Override
			public IProgressMonitor createMonitor(final Job job) {
				final StatusUpdateProgressMonitor statusUpdateProgressMonitor = new StatusUpdateProgressMonitor();
				return statusUpdateProgressMonitor.addJob(job);
			}
		});
	}

	private final class StatusUpdateProgressMonitor extends NullProgressMonitor {

		// thread-Safe via thread confinement of the UI-Thread
		// (means access only via UI-Thread)
		private long runningTasks = 0L;

		@Override
		public void beginTask(final String name, final int totalWork) {
			sync.syncExec(() -> {
				if (runningTasks <= 0) {
					// --- no task is running at the moment ---
					progressBar.setSelection(0);
					progressBar.setMaximum(totalWork);
				} else {
					// --- other tasks are running ---
					progressBar.setMaximum(progressBar.getMaximum() + totalWork);
				}

				runningTasks++;
				progressBar.setToolTipText(name);
			});
		}

		@Override
		public void worked(final int work) {
			sync.syncExec(() -> progressBar.setSelection(progressBar.getSelection() + work));
		}
		
		@Override
		public void setTaskName(final String name) {
			sync.syncExec(() -> progressBar.setToolTipText(name));
		}
		
		public IProgressMonitor addJob(final Job job) {
			if (job != null) {
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(final IJobChangeEvent event) {
						sync.syncExec(() -> {
							runningTasks--;
							if (progressBar.isDisposed())
								return;
							
							if (runningTasks > 0) {
								// --- some tasks are still running ---
								progressBar.setToolTipText(NLS.bind(Messages.ProgressMonitorControl_RunningTasks, runningTasks));

							} else {
								// --- all tasks are done ---
								progressBar.setToolTipText("");
								progressBar.setSelection(0);
							}
						});

						// clean-up
						event.getJob().removeJobChangeListener(this);
					}
				});
			}
			return this;
		}
	}
}
