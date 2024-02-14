package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.operations.Remedy;
import org.eclipse.equinox.p2.operations.RemedyIUDetail;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.internal.ui.model.RemedyElementCategory;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

public class RemediationGroup {
	static final int ALLOWPARTIALINSTALL_INDEX = 0;
	static final int ALLOWDIFFERENTVERSION_INDEX = 1;
	static final int ALLOWINSTALLEDUPDATE_INDEX = 2;
	static final int ALLOWINSTALLEDREMOVAL_INDEX = 3;

	private RemediationOperation remediationOperation;
	Composite remediationComposite;
	private Button bestBeingInstalledRelaxedButton;
	private Button bestInstalledRelaxedButton;
	Button buildMyOwnSolution;
	final ArrayList<Button> checkboxes = new ArrayList<>();
	private Composite resultFoundComposite;
	private Composite resultComposite;
	private Composite resultNotFoundComposite;
	private Composite resultErrorComposite;
	private Remedy currentRemedy;

	private TreeViewer treeViewer;
	private TreeViewerComparator treeComparator;
	protected IUElementListRoot input;
	private StackLayout switchRemediationLayout;
	Group detailsControl;
	Text detailStatusText;
	Composite checkBoxesComposite;
	private IUDetailsGroup iuDetailsGroup;

	HashMap<String, String[]> CONSTRAINTS;
	private WizardPage containerPage;
	private IEclipseContext ctx;

	public class RemedyContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// not needed
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// not needed
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			final Object[] elements = ElementUtils.requestToRemedyElementsCategories((Remedy) inputElement);
			return elements;
		}

		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof RemedyElementCategory) {
				final RemedyElementCategory category = (RemedyElementCategory) parentElement;
				return category.getElements().toArray();
			}
			return null;
		}

		@Override
		public Object getParent(final Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(final Object element) {
			if (element instanceof RemedyElementCategory) {
				return true;
			}
			return false;
		}
	}

	public RemediationGroup(final IEclipseContext ctx, final WizardPage page) {
		this.ctx = ctx;
		CONSTRAINTS = new HashMap<>();
		CONSTRAINTS.put(ProvUIMessages.RemediationPage_BeingInstalledSection, new String[] {ProvUIMessages.RemediationPage_BeingInstalledSection_AllowPartialInstall, ProvUIMessages.RemediationPage_BeingInstalledSection_AllowDifferentVersion});
		CONSTRAINTS.put(ProvUIMessages.RemediationPage_InstalledSection, new String[] {ProvUIMessages.RemediationPage_InstalledSection_AllowInstalledUpdate, ProvUIMessages.RemediationPage_InstalledSection_AllowInstalledRemoval});

		containerPage = page;
	}

	public Composite getComposite() {
		return remediationComposite;
	}

	public void createRemediationControl(final Composite container) {
		remediationComposite = new Composite(container, SWT.NONE);
		remediationComposite.setLayout(new GridLayout());
		Listener solutionslistener;

		final Label descriptionLabel = new Label(remediationComposite, SWT.NONE);
		descriptionLabel.setText(ProvUIMessages.RemediationPage_SubDescription);

		solutionslistener = e -> {
			final Button btn = (Button) e.widget;
			final Remedy remedy = (btn.getData() == null ? null : (Remedy) btn.getData());
			checkboxes.get(ALLOWPARTIALINSTALL_INDEX).setSelection(remedy != null && remedy.getConfig().allowPartialInstall);
			checkboxes.get(ALLOWDIFFERENTVERSION_INDEX).setSelection(remedy != null && remedy.getConfig().allowDifferentVersion);
			checkboxes.get(ALLOWINSTALLEDUPDATE_INDEX).setSelection(remedy != null && remedy.getConfig().allowInstalledUpdate);
			checkboxes.get(ALLOWINSTALLEDREMOVAL_INDEX).setSelection(remedy != null && remedy.getConfig().allowInstalledRemoval);
			for (final Button btn1 : checkboxes) {
				btn1.setVisible(true);
			}
			if (btn == buildMyOwnSolution && btn.getSelection()) {
				checkBoxesComposite.setVisible(true);
				((GridData) checkBoxesComposite.getLayoutData()).exclude = false;
			} else {
				checkBoxesComposite.setVisible(false);
				((GridData) checkBoxesComposite.getLayoutData()).exclude = true;
			}
			currentRemedy = searchRemedyMatchingUserChoices();
			refreshResultComposite();
			remediationComposite.layout(false);
		};

		bestBeingInstalledRelaxedButton = new Button(remediationComposite, SWT.RADIO);
		bestBeingInstalledRelaxedButton.setText(ProvUIMessages.RemediationPage_BestSolutionBeingInstalledRelaxed);
		bestBeingInstalledRelaxedButton.addListener(SWT.Selection, solutionslistener);

		bestInstalledRelaxedButton = new Button(remediationComposite, SWT.RADIO);
		bestInstalledRelaxedButton.setText(ProvUIMessages.RemediationPage_BestSolutionInstallationRelaxed);
		bestInstalledRelaxedButton.addListener(SWT.Selection, solutionslistener);

		buildMyOwnSolution = new Button(remediationComposite, SWT.RADIO);
		buildMyOwnSolution.setText(ProvUIMessages.RemediationPage_BestSolutionBuilt);
		buildMyOwnSolution.addListener(SWT.Selection, solutionslistener);

		final Listener checkboxListener = e -> {
			currentRemedy = searchRemedyMatchingUserChoices();
			refreshResultComposite();
		};
		checkBoxesComposite = new Composite(remediationComposite, SWT.NONE);
		checkBoxesComposite.setLayout(new GridLayout(1, false));
		GridData data = new GridData();
		data.exclude = false;
		data.horizontalAlignment = SWT.FILL;
		checkBoxesComposite.setLayoutData(data);

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 30;
		final Iterator<String> iter = CONSTRAINTS.keySet().iterator();
		while (iter.hasNext()) {
			final String key = iter.next();
			final String[] values = CONSTRAINTS.get(key);
			for (final String value : values) {
				final Button checkBtn = new Button(checkBoxesComposite, SWT.CHECK);
				checkBtn.setText(value);
				checkBtn.setData(value);
				checkBtn.setLayoutData(gd);
				checkBtn.addListener(SWT.Selection, checkboxListener);
				checkboxes.add(checkBtn);
			}

		}

		resultComposite = new Composite(remediationComposite, SWT.NONE);
		// GridLayoutFactory.fillDefaults().numColumns(1).applyTo(resultComposite);
		switchRemediationLayout = new StackLayout();
		resultComposite.setLayout(switchRemediationLayout);
		final GridData data1 = new GridData(GridData.FILL_BOTH);
		resultComposite.setLayoutData(data1);

		resultErrorComposite = new Composite(resultComposite, SWT.NONE);
		resultErrorComposite.setLayout(new GridLayout());
		resultErrorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		resultNotFoundComposite = new Composite(resultComposite, SWT.NONE);
		resultNotFoundComposite.setLayout(new GridLayout());
		final Label resultNotFoundLabel = new Label(resultNotFoundComposite, SWT.NONE);
		resultNotFoundLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		resultNotFoundLabel.setText(ProvUIMessages.RemediationPage_NoSolutionFound);

		resultFoundComposite = new Composite(resultComposite, SWT.NONE);
		resultFoundComposite.setLayout(new GridLayout());

		final Group insideFoundComposite = new Group(resultFoundComposite, SWT.NONE);
		insideFoundComposite.setText(ProvUIMessages.RemediationPage_SolutionDetails);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		insideFoundComposite.setLayout(gridLayout);
		insideFoundComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		treeViewer = new TreeViewer(insideFoundComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		data = new GridData(GridData.FILL_BOTH);
		final Tree tree = treeViewer.getTree();
		tree.setLayoutData(data);
		tree.setHeaderVisible(true);
		final TreeViewerColumn nameColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		nameColumn.getColumn().setText(ProvUIMessages.ProvUI_NameColumnTitle);
		nameColumn.getColumn().setWidth(400);
		nameColumn.getColumn().setMoveable(true);
		nameColumn.getColumn().addSelectionListener(columnChangeListener(0));
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				if (element instanceof RemedyElementCategory)
					return ((RemedyElementCategory) element).getName();
				if (element instanceof RemedyIUDetail) {
					final RemedyIUDetail iu = (RemedyIUDetail) element;
					String label = iu.getIu().getProperty(IInstallableUnit.PROP_NAME, null);
					if (label == null)
						label = iu.getIu().getId();
					return label;
				}
				return super.getText(element);
			}

			@Override
			public Image getImage(final Object element) {
				if (element instanceof RemedyElementCategory) {
					final RemedyElementCategory category = (RemedyElementCategory) element;
					if (category.getName().equals(ProvUIMessages.RemedyCategoryAdded))
						return ProvUIImages.getImage(ctx, ProvUIImages.IMG_ADDED);
					if (category.getName().equals(ProvUIMessages.RemedyCategoryChanged))
						return ProvUIImages.getImage(ctx, ProvUIImages.IMG_CHANGED);
					if (category.getName().equals(ProvUIMessages.RemedyCategoryNotAdded))
						return ProvUIImages.getImage(ctx, ProvUIImages.IMG_NOTADDED);
					if (category.getName().equals(ProvUIMessages.RemedyCategoryRemoved))
						return ProvUIImages.getImage(ctx, ProvUIImages.IMG_REMOVED);
				} else if (element instanceof RemedyIUDetail) {
					final RemedyIUDetail iuDetail = (RemedyIUDetail) element;
					final int status = compare(iuDetail);
					if (compare(iuDetail.getBeingInstalledVersion(), iuDetail.getRequestedVersion()) < 0 && containerPage != null && containerPage.getWizard() instanceof UpdateWizard) {
						final Image img = ProvUIImages.getImage(ctx, ProvUIImages.IMG_UPGRADED_IU);
						final ImageDescriptor overlay = ProvUIImages.getImageDescriptor(ctx, ProvUIImages.IMG_INFO);
						final String decoratedImageId = ProvUIImages.IMG_UPGRADED_IU.concat(ProvUIImages.IMG_INFO);
						if (ProvUIImages.getImage(ctx, decoratedImageId) == null) {
							final DecorationOverlayIcon decoratedImage = new DecorationOverlayIcon(img, overlay, IDecoration.BOTTOM_RIGHT);
							((ImageRegistry) ctx.get(ProvUIAddon.IMG_REGISTRY)).put(decoratedImageId, decoratedImage);
						}
						final Image decoratedImg = ProvUIImages.getImage(ctx, decoratedImageId);
						return decoratedImg;
					}

					if (status < 0)
						return ProvUIImages.getImage(ctx, ProvUIImages.IMG_DOWNGRADED_IU);
					if (status > 0)
						return ProvUIImages.getImage(ctx, ProvUIImages.IMG_UPGRADED_IU);
					return ProvUIImages.getImage(ctx, ProvUIImages.IMG_IU);
				}
				return super.getImage(element);
			}

			@Override
			public String getToolTipText(final Object element) {
				if (element instanceof RemedyIUDetail) {
					final RemedyIUDetail iuDetail = (RemedyIUDetail) element;
					String toolTipText = ""; //$NON-NLS-1$
					final List<String> versions = new ArrayList<>();
					if (iuDetail.getInstalledVersion() != null)
						versions.add(ProvUIMessages.RemedyElementInstalledVersion + iuDetail.getInstalledVersion().toString());
					if (iuDetail.getRequestedVersion() != null)
						versions.add(ProvUIMessages.RemedyElementRequestedVersion + iuDetail.getRequestedVersion().toString());
					if (iuDetail.getBeingInstalledVersion() != null)
						versions.add(ProvUIMessages.RemedyElementBeingInstalledVersion + iuDetail.getBeingInstalledVersion().toString());
					for (final String version : versions) {
						toolTipText += (toolTipText == "" ? "" : "\n") + version; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					if (containerPage != null && containerPage.getWizard() instanceof UpdateWizard && compare(iuDetail.getBeingInstalledVersion(), iuDetail.getRequestedVersion()) < 0)
						toolTipText = ProvUIMessages.RemedyElementNotHighestVersion + "\n\n" + toolTipText; //$NON-NLS-1$
					return toolTipText;
				}
				return super.getToolTipText(element);
			}

			private int compare(final Version versionA, final Version versionB) {
				if (versionA != null && versionB != null)
					return versionA.compareTo(versionB);
				return 0;
			}

			private int compare(final RemedyIUDetail iuDetail) {
				if (iuDetail.getStatus() == RemedyIUDetail.STATUS_ADDED && iuDetail.getRequestedVersion() != null && iuDetail.getBeingInstalledVersion() != null) {
					return compare(iuDetail.getBeingInstalledVersion(), iuDetail.getRequestedVersion());
				}
				if (iuDetail.getStatus() == RemedyIUDetail.STATUS_CHANGED && iuDetail.getInstalledVersion() != null && iuDetail.getBeingInstalledVersion() != null) {
					return compare(iuDetail.getBeingInstalledVersion(), iuDetail.getInstalledVersion());
				}
				return 0;
			}
		});
		final TreeViewerColumn versionColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		versionColumn.getColumn().setText(ProvUIMessages.ProvUI_VersionColumnTitle);
		versionColumn.getColumn().setWidth(200);
		versionColumn.getColumn().addSelectionListener(columnChangeListener(1));
		versionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				if (element instanceof RemedyIUDetail) {
					final RemedyIUDetail iu = (RemedyIUDetail) element;
					if (iu.getBeingInstalledVersion() != null)
						return iu.getBeingInstalledVersion().toString();
				}
				return ""; //$NON-NLS-1$
			}
		});
		final TreeViewerColumn idColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		idColumn.getColumn().setText(ProvUIMessages.ProvUI_IdColumnTitle);
		idColumn.getColumn().setWidth(200);
		idColumn.getColumn().addSelectionListener(columnChangeListener(2));

		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				if (element instanceof RemedyIUDetail) {
					final RemedyIUDetail iu = (RemedyIUDetail) element;
					return iu.getIu().getId();
				}
				return ""; //$NON-NLS-1$
			}
		});

		treeComparator = new TreeViewerComparator();
		treeViewer.setComparator(treeComparator);
		treeViewer.setContentProvider(new RemedyContentProvider());
		treeViewer.setAutoExpandLevel(2);
		iuDetailsGroup = new IUDetailsGroup(resultErrorComposite, treeViewer, 500, true);
	}

	public IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	private Remedy searchBestDefaultRemedy() {
		if (remediationOperation.bestSolutionChangingTheRequest() != null) {
			return remediationOperation.bestSolutionChangingTheRequest();
		}
		if (remediationOperation.bestSolutionChangingWhatIsInstalled() != null) {
			return remediationOperation.bestSolutionChangingWhatIsInstalled();
		}
		return remediationOperation.getRemedies().get(0);
	}

	public void update(final RemediationOperation operation) {
		Assert.isNotNull(operation, "operation"); //$NON-NLS-1$
		this.remediationOperation = operation;
		currentRemedy = searchBestDefaultRemedy();

		bestBeingInstalledRelaxedButton.setData(remediationOperation.bestSolutionChangingTheRequest());
		bestInstalledRelaxedButton.setData(remediationOperation.bestSolutionChangingWhatIsInstalled());

		bestBeingInstalledRelaxedButton.setEnabled(remediationOperation.bestSolutionChangingTheRequest() != null);
		bestInstalledRelaxedButton.setEnabled(remediationOperation.bestSolutionChangingWhatIsInstalled() != null);
		bestBeingInstalledRelaxedButton.setSelection(false);
		bestInstalledRelaxedButton.setSelection(false);
		buildMyOwnSolution.setSelection(false);

		if (currentRemedy == remediationOperation.bestSolutionChangingTheRequest()) {
			bestBeingInstalledRelaxedButton.setSelection(true);
			bestBeingInstalledRelaxedButton.notifyListeners(SWT.Selection, new Event());
		} else if (currentRemedy == remediationOperation.bestSolutionChangingWhatIsInstalled()) {
			bestInstalledRelaxedButton.setSelection(true);
			bestInstalledRelaxedButton.notifyListeners(SWT.Selection, new Event());
		} else {
			buildMyOwnSolution.setData(currentRemedy);
			buildMyOwnSolution.setSelection(true);
			buildMyOwnSolution.notifyListeners(SWT.Selection, new Event());
		}
	}

	private boolean isContraintOK(final int btnIndex, final boolean value) {
		return (checkboxes.get(btnIndex).getSelection() && value) || (!checkboxes.get(btnIndex).getSelection() && !value);
	}

	Remedy searchRemedyMatchingUserChoices() {
		if (remediationOperation == null) {
			ctx.get(ILog.class).log(new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, "RemediationOperation was not initialized yet.")); //$NON-NLS-1$
			return null;
		}
		for (final Remedy remedy : remediationOperation.getRemedies()) {
			if (isContraintOK(ALLOWPARTIALINSTALL_INDEX, remedy.getConfig().allowPartialInstall) && isContraintOK(ALLOWDIFFERENTVERSION_INDEX, remedy.getConfig().allowDifferentVersion) && isContraintOK(ALLOWINSTALLEDUPDATE_INDEX, remedy.getConfig().allowInstalledUpdate) && isContraintOK(ALLOWINSTALLEDREMOVAL_INDEX, remedy.getConfig().allowInstalledRemoval)) {
				if (remedy.getRequest() != null) {
					return remedy;
				}
			}
		}
		return null;
	}

	void refreshResultComposite() {
		resultComposite.setVisible(true);
		if (!checkboxes.get(ALLOWPARTIALINSTALL_INDEX).getSelection() && !checkboxes.get(ALLOWDIFFERENTVERSION_INDEX).getSelection() && !checkboxes.get(ALLOWINSTALLEDUPDATE_INDEX).getSelection() && !checkboxes.get(ALLOWINSTALLEDREMOVAL_INDEX).getSelection()) {
			switchRemediationLayout.topControl = resultErrorComposite;
		} else {

			if (currentRemedy == null) {
				switchRemediationLayout.topControl = resultNotFoundComposite;
			} else {
				if (currentRemedy.getIusDetails().size() == 0) {
					switchRemediationLayout.topControl = resultNotFoundComposite;
				} else {
					treeViewer.setInput(currentRemedy);
					switchRemediationLayout.topControl = resultFoundComposite;
				}
			}
		}
		resultComposite.layout();
		containerPage.setPageComplete(currentRemedy != null);
	}

	public Remedy getCurrentRemedy() {
		return currentRemedy;
	}

	public String getMessage() {
		return ProvUIMessages.InstallRemediationPage_Description;
	}

	class TreeViewerComparator extends ViewerComparator {
		private int sortColumn = 0;
		private int ascending = 1;

		@Override
		public int compare(final Viewer viewer1, final Object e1, final Object e2) {
			if (!(e1 instanceof RemedyIUDetail && e2 instanceof RemedyIUDetail))
				return 0;
			final IInstallableUnit iu1 = ((RemedyIUDetail) e1).getIu();
			final IInstallableUnit iu2 = ((RemedyIUDetail) e2).getIu();
			if (iu1 != null && iu2 != null) {
				if (viewer1 instanceof TreeViewer) {
					final TreeViewer theTreeViewer = (TreeViewer) viewer1;
					final ColumnLabelProvider labelProvider = (ColumnLabelProvider) theTreeViewer.getLabelProvider(sortColumn);
					final String e1p = labelProvider.getText(e1);
					final String e2p = labelProvider.getText(e2);
					// don't suppress this warning as it will cause build-time warning
					// see bug 423628. This should be possible to fix once
					// SWT/JFace adopts generics
					return ascending * getComparator().compare(e1p, e2p);
				}
				return 0;
			}
			return super.compare(viewer1, e1, e2);
		}

		public void setSortColumn(final int sortColumn) {
			this.sortColumn = sortColumn;
		}

		public int getSortColumn() {
			return sortColumn;
		}

		public boolean isAscending() {
			return ascending == 1 ? true : false;
		}

		public void setAscending(final boolean asc) {
			this.ascending = asc ? 1 : -1;
		}
	}

	private SelectionListener columnChangeListener(final int index) {
		return SelectionListener.widgetSelectedAdapter(e -> updateTableSorting(index));
	}

	private void updateTableSorting(final int columnIndex) {
		final TreeViewerComparator comparator = (TreeViewerComparator) treeViewer.getComparator();
		// toggle direction if it's the same column
		if (columnIndex == treeComparator.getSortColumn()) {
			comparator.setAscending(!treeComparator.isAscending());
		}
		comparator.setSortColumn(columnIndex);
		treeViewer.getTree().setSortColumn(treeViewer.getTree().getColumn(columnIndex));
		treeViewer.getTree().setSortDirection(comparator.isAscending() ? SWT.UP : SWT.DOWN);
		treeViewer.refresh(false);
		treeViewer.expandToLevel(2);
	}

}
