package ro.linic.ui.p2.internal.ui.dialogs;

import java.text.NumberFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 *
 * @since 3.5
 */
public abstract class SizeComputingWizardPage extends ResolutionResultsWizardPage {
	protected Label sizeInfo;
	protected long size;
	Job sizingJob;
	private IProvisioningPlan lastComputedPlan = null;

	protected SizeComputingWizardPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard,
			final IUElementListRoot root, final ProfileChangeOperation initialResolution) {
		super(ctx, ui, wizard, root, initialResolution);
		// Compute size immediately if a plan is available.  This may or may not finish before
		// the widgetry is created.
		if (initialResolution != null && initialResolution.hasResolved())
			computeSizing(initialResolution.getProvisioningPlan(), initialResolution.getProvisioningContext());
		else
			// Set the size to indicate there is no size yet.
			size = ProvUI.SIZE_NOTAPPLICABLE;
	}

	protected void computeSizing(final IProvisioningPlan plan, final ProvisioningContext provisioningContext) {
		if (plan == lastComputedPlan)
			return;
		lastComputedPlan = plan;
		size = ProvUI.SIZE_UNKNOWN;
		updateSizingInfo();
		if (sizingJob != null)
			sizingJob.cancel();
		sizingJob = new Job(ProvUIMessages.SizeComputingWizardPage_SizeJobTitle) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				size = ProvUI.getSize(ProvUI.getEngine(getProvisioningUI().getSession()), plan, provisioningContext, monitor);
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				if (display != null) {
					display.asyncExec(() -> updateSizingInfo());
				}
				return Status.OK_STATUS;
			}

		};
		sizingJob.setUser(false);
		sizingJob.setSystem(true);
		sizingJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER, Boolean.toString(true));
		sizingJob.schedule();
	}

	@Override
	protected void createSizingInfo(final Composite parent) {
		sizeInfo = new Label(parent, SWT.NONE);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		sizeInfo.setLayoutData(data);
		updateSizingInfo();
	}

	protected void updateSizingInfo() {
		if (sizeInfo != null && !sizeInfo.isDisposed()) {
			if (size == ProvUI.SIZE_NOTAPPLICABLE)
				sizeInfo.setVisible(false);
			else {
				sizeInfo.setText(NLS.bind(ProvUIMessages.UpdateOrInstallWizardPage_Size, getFormattedSize()));
				sizeInfo.setVisible(true);
			}
		}
	}

	protected String getFormattedSize() {
		if (size == ProvUI.SIZE_UNKNOWN || size == ProvUI.SIZE_UNAVAILABLE)
			return ProvUIMessages.IUDetailsLabelProvider_Unknown;
		if (size > 1000L) {
			final long kb = size / 1000L;
			return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_KB, NumberFormat.getInstance().format(Long.valueOf(kb)));
		}
		return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_Bytes, NumberFormat.getInstance().format(Long.valueOf(size)));
	}

	@Override
	public void dispose() {
		if (sizingJob != null) {
			sizingJob.cancel();
			sizingJob = null;
		}
	}

	@Override
	public void updateStatus(final IUElementListRoot root, final ProfileChangeOperation op) {
		super.updateStatus(root, op);
		if (op != null && op.getProvisioningPlan() != null)
			computeSizing(op.getProvisioningPlan(), op.getProvisioningContext());
	}

	@Override
	protected IQueryable<IInstallableUnit> getQueryable(final IProvisioningPlan plan) {
		return plan.getAdditions();
	}
}
