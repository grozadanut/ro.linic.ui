
package ro.linic.ui.base.handler;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class UpdateHandler {
	private static final String REPOSITORY_LOC = System.getProperty("UpdateHandler.Repo",
	        "file:/C:/work/repository");
	
	private boolean cancelled = false;
	
	
	static IQueryResult<IInstallableUnit> getInstallableUnits(final IProvisioningAgent agent, final URI location, final IQuery<IInstallableUnit> query, final IProgressMonitor monitor) {
		IQueryable<IInstallableUnit> queryable = null;
		if (location == null) {
			queryable = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		} else {
			queryable = getMetadataRepository(agent, location);
		}
		if (queryable != null)
			return queryable.query(query, monitor);
		return null;
	}
	
	static IMetadataRepository getMetadataRepository(final IProvisioningAgent agent, final URI location) {
		final IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		try {
			return manager.loadRepository(location, null);
		} catch (final ProvisionException e) {
			return null;
		}
	}
	
//	static void test() {
//		URI uri = null;
//	    try {
//	        uri = new URI(REPOSITORY_LOC);
//	        final IQueryResult<IInstallableUnit> units = getInstallableUnits(agent, uri, QueryUtil.ALL_UNITS, monitor);
//	        System.out.println(units.iterator().next());
//	    } catch (final URISyntaxException e) {
//	        e.printStackTrace();
//	    }
//	}
	
	
	
	@Execute
	public void execute(final IProvisioningAgent agent, final UISynchronize sync, final IWorkbench workbench) {
		// update using a progress monitor
		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				update(agent, monitor, sync, workbench);
			}
		};

		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private IStatus update(final IProvisioningAgent agent, final IProgressMonitor monitor, final UISynchronize sync,
			final IWorkbench workbench) {
		final ProvisioningSession session = new ProvisioningSession(agent);
		// update the whole running profile, otherwise specify IUs
		final UpdateOperation operation = new UpdateOperation(session);
		configureUpdate(operation);
		
		final SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);

		// check if updates are available
		final IStatus status = operation.resolveModal(sub.newChild(100));
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			showMessage(sync, "Nothing to update");
			return Status.CANCEL_STATUS;
		} else {
			final ProvisioningJob provisioningJob = operation.getProvisioningJob(sub.newChild(100));
			if (provisioningJob != null) {
				sync.syncExec(new Runnable() {
					@Override public void run() {
						final boolean performUpdate = MessageDialog.openQuestion(null, "Updates available",
								"There are updates available. Do you want to install them now?");
						if (performUpdate) {
							provisioningJob.addJobChangeListener(new JobChangeAdapter() {
								@Override public void done(final IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										sync.syncExec(new Runnable() {
											@Override
											public void run() {
												final boolean restart = MessageDialog.openQuestion(null,
														"Updates installed, restart?",
														"Updates have been installed successfully, do you want to restart?");
												if (restart) {
													workbench.restart();
												}
											}
										});
									} else {
										showError(sync, event.getResult().getMessage());
										cancelled = true;
									}
								}
							});

							// since we switched to the UI thread for interacting with the user
							// we need to schedule the provisioning thread, otherwise it would
							// be executed also in the UI thread and not in a background thread
							provisioningJob.schedule();
						} else {
							cancelled = true;
						}
					}
				});
			} else {
				if (operation.hasResolved()) {
					showError(sync, "Couldn't get provisioning job: " + operation.getResolutionResult());
				} else {
					showError(sync, "Couldn't resolve provisioning job");
				}
				cancelled = true;
			}
		}

		if (cancelled) {
			// reset cancelled flag
			cancelled = false;
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	private void showMessage(final UISynchronize sync, final String message) {
		// as the provision needs to be executed in a background thread
		// we need to ensure that the message dialog is executed in
		// the UI thread
		sync.syncExec(new Runnable() {
			@Override public void run() {
				MessageDialog.openInformation(null, "Information", message);
			}
		});
	}

	private void showError(final UISynchronize sync, final String message) {
		// as the provision needs to be executed in a background thread
		// we need to ensure that the message dialog is executed in
		// the UI thread
		sync.syncExec(new Runnable() {
			@Override public void run() {
				MessageDialog.openError(null, "Error", message);
			}
		});
	}
	
	private UpdateOperation configureUpdate(final UpdateOperation operation) {
	    // create uri and check for validity
	    URI uri = null;
	    try {
	        uri = new URI(REPOSITORY_LOC);
	    } catch (final URISyntaxException e) {
	        e.printStackTrace();
	        return operation;
	    }

	    // set location of artifact and metadata repo
	    operation.getProvisioningContext().setArtifactRepositories(new URI[] { uri });
	    operation.getProvisioningContext().setMetadataRepositories(new URI[] { uri });
	    return operation;
	}
}