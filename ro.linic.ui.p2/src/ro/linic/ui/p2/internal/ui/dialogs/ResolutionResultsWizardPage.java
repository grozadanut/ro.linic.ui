package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.AvailableIUElement;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IIUElement;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.internal.ui.model.ProvElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElement;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.internal.ui.viewers.IUComparator;
import ro.linic.ui.p2.internal.ui.viewers.IUDetailsLabelProvider;
import ro.linic.ui.p2.internal.ui.viewers.ProvElementComparer;
import ro.linic.ui.p2.internal.ui.viewers.ProvElementContentProvider;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * A wizard page that shows detailed information about a resolved install
 * operation.  It allows drill down into the elements that will be installed.
 *
 * @since 3.4
 *
 */
public abstract class ResolutionResultsWizardPage extends ResolutionStatusPage {

	private static final String DIALOG_SETTINGS_SECTION = "ResolutionResultsPage"; //$NON-NLS-1$

	protected IUElementListRoot input;
	ProfileChangeOperation resolvedOperation;
	TreeViewer treeViewer;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	private IUDetailsGroup iuDetailsGroup;
	SashForm sashForm;

	protected ResolutionResultsWizardPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard, final IUElementListRoot input, final ProfileChangeOperation operation) {
		super(ctx, "ResolutionPage", ui, wizard); //$NON-NLS-1$
		this.resolvedOperation = operation;
		if (input == null)
			this.input = new IUElementListRoot(ctx, ui);
		else
			this.input = input;
	}

	@Override
	public void createControl(final Composite parent) {
		display = parent.getDisplay();
		sashForm = new SashForm(parent, SWT.VERTICAL);
		final FillLayout layout = new FillLayout();
		sashForm.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		sashForm.setLayoutData(data);
		initializeDialogUnits(sashForm);

		final Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		composite.setLayout(gridLayout);

		treeViewer = createTreeViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		final Tree tree = treeViewer.getTree();
		tree.setLayoutData(data);
		tree.setHeaderVisible(true);
		activateCopy(tree);
		final TreeViewerColumn nameColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		nameColumn.getColumn().setText(ProvUIMessages.ProvUI_NameColumnTitle);
		nameColumn.getColumn().setWidth(400);
		nameColumn.getColumn().setMoveable(true);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				String label = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if (label == null)
					label = iu.getId();
				return label;
			}

			@Override
			public Image getImage(final Object element) {
				if (element instanceof ProvElement)
					return ((ProvElement) element).getImage(element);
				if (ProvUI.getAdapter(element, IInstallableUnit.class) != null)
					return ProvUIImages.getImage(ctx, ProvUIImages.IMG_IU);
				return null;
			}

			@Override
			public String getToolTipText(final Object element) {
				if (element instanceof AvailableIUElement && ((AvailableIUElement) element).getImageOverlayId(null) == ProvUIImages.IMG_INFO)
					return ProvUIMessages.RemedyElementNotHighestVersion;
				return super.getToolTipText(element);
			}
		});
		final TreeViewerColumn versionColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		versionColumn.getColumn().setText(ProvUIMessages.ProvUI_VersionColumnTitle);
		versionColumn.getColumn().setWidth(200);
		versionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				if (element instanceof IIUElement) {
					if (((IIUElement) element).shouldShowVersion())
						return iu.getVersion().toString();
					return ""; //$NON-NLS-1$
				}
				return iu.getVersion().toString();
			}
		});
		final TreeViewerColumn idColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		idColumn.getColumn().setText(ProvUIMessages.ProvUI_IdColumnTitle);
		idColumn.getColumn().setWidth(200);

		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				return iu.getId();
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		final IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		treeViewer.setComparator(comparator);
		treeViewer.setComparer(new ProvElementComparer());
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		contentProvider = new ProvElementContentProvider();
		treeViewer.setContentProvider(contentProvider);
		//		labelProvider = new IUDetailsLabelProvider(null, getColumnConfig(), getShell());
		//		treeViewer.setLabelProvider(labelProvider);

		// Optional area to show the size
		createSizingInfo(composite);

		// The text area shows a description of the selected IU, or error detail if applicable.
		iuDetailsGroup = new IUDetailsGroup(sashForm, treeViewer, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

		setControl(sashForm);
		sashForm.setWeights(getSashWeights());
		Dialog.applyDialogFont(sashForm);

		// Controls for filtering/presentation/site selection
		final Composite controlsComposite = new Composite(composite, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		controlsComposite.setLayout(layout);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		controlsComposite.setLayoutData(gd);

		final Runnable runnable = () -> {
			treeViewer.addSelectionChangedListener(event -> setDetailText(resolvedOperation));
			setDrilldownElements(input, resolvedOperation);
			treeViewer.setInput(input);
		};

		if (resolvedOperation != null && !resolvedOperation.hasResolved()) {
			try {
				getContainer().run(true, false, monitor -> {
					resolvedOperation.resolveModal(monitor);
					display.asyncExec(runnable);
				});
			} catch (final Exception e) {
				ProvUI.reportStatus(ctx, new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, e.getMessage(), e),
						StatusManager.SHOW);
			}
		} else {
			runnable.run();
		}
	}

	@Override
	public void updateStatus(final IUElementListRoot newRoot, final ProfileChangeOperation op) {
		super.updateStatus(newRoot, op);
	}

	protected void createSizingInfo(final Composite parent) {
		// Default is to do nothing
	}

	public boolean performFinish() {
		if (resolvedOperation.getResolutionResult().getSeverity() != IStatus.ERROR) {
			getProvisioningUI().schedule(resolvedOperation.getProvisioningJob(null), StatusManager.SHOW | StatusManager.LOG);
			return true;
		}
		return false;
	}

	protected TreeViewer getTreeViewer() {
		return treeViewer;
	}

	public IProvisioningPlan getCurrentPlan() {
		if (resolvedOperation != null)
			return resolvedOperation.getProvisioningPlan();
		return null;
	}

	@Override
	protected Object[] getSelectedElements() {
		return treeViewer.getStructuredSelection().toArray();
	}

	@Override
	protected IInstallableUnit getSelectedIU() {
		final java.util.List<IInstallableUnit> units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.size() == 0)
			return null;
		return units.get(0);
	}

	@Override
	protected boolean shouldCompleteOnCancel() {
		return false;
	}

	protected Collection<IInstallableUnit> getIUs() {
		return ElementUtils.elementsToIUs(input.getChildren(input));
	}

	void setDrilldownElements(final IUElementListRoot root, final ProfileChangeOperation operation) {
		if (operation == null || operation.getProvisioningPlan() == null)
			return;
		final Object[] elements = root.getChildren(root);
		for (final Object element : elements) {
			if (element instanceof QueriedElement) {
				((QueriedElement) element).setQueryable(getQueryable(operation.getProvisioningPlan()));
			}
		}
	}

	protected abstract String getOperationLabel();

	/**
	 * Returns the restart policy for this operation.
	 *
	 * @return an integer constant describing whether the running profile
	 * needs to be restarted.
	 *
	 * @see ProvisioningJob#RESTART_NONE
	 * @see ProvisioningJob#RESTART_ONLY
	 * @see ProvisioningJob#RESTART_OR_APPLY
	 *
	 */
	protected int getRestartPolicy() {
		return ProvisioningJob.RESTART_OR_APPLY;
	}

	/**
	 * Returns the task name for this operation, or <code>null</code> to display
	 * a generic task name.
	 */
	protected String getOperationTaskName() {
		return null;
	}

	protected TreeViewer createTreeViewer(final Composite parent) {
		return new TreeViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
	}

	protected abstract IQueryable<IInstallableUnit> getQueryable(IProvisioningPlan plan);

	@Override
	protected String getClipboardText(final Control control) {
		return CopyUtils.getIndentedClipboardText(getSelectedElements(), labelProvider);
	}

	@Override
	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	@Override
	protected boolean isCreated() {
		return treeViewer != null;
	}

	@Override
	protected void updateCaches(final IUElementListRoot newRoot, final ProfileChangeOperation op) {
		resolvedOperation = op;
		if (newRoot != null) {
			setDrilldownElements(newRoot, resolvedOperation);
			if (treeViewer != null) {
				if (input != newRoot)
					treeViewer.setInput(newRoot);
				else
					treeViewer.refresh();
			}
			input = newRoot;
		}
	}

	@Override
	protected String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	@Override
	protected int getColumnWidth(final int index) {
		return treeViewer.getTree().getColumn(index).getWidth();
	}

	@Override
	protected SashForm getSashForm() {
		return sashForm;
	}
}