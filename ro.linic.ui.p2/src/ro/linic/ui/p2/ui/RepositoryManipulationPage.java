package ro.linic.ui.p2.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.ProvUIProvisioningListener;
import ro.linic.ui.p2.internal.ui.QueryProvider;
import ro.linic.ui.p2.internal.ui.dialogs.AddRepositoryDialog;
import ro.linic.ui.p2.internal.ui.dialogs.CopyUtils;
import ro.linic.ui.p2.internal.ui.dialogs.ILayoutConstants;
import ro.linic.ui.p2.internal.ui.dialogs.RepositoryManipulatorDropTarget;
import ro.linic.ui.p2.internal.ui.dialogs.RepositoryNameAndLocationDialog;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.MetadataRepositories;
import ro.linic.ui.p2.internal.ui.model.MetadataRepositoryElement;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.internal.ui.viewers.MetadataRepositoryElementComparator;
import ro.linic.ui.p2.internal.ui.viewers.ProvElementComparer;
import ro.linic.ui.p2.internal.ui.viewers.ProvElementContentProvider;
import ro.linic.ui.p2.internal.ui.viewers.RepositoryDetailsLabelProvider;
import ro.linic.ui.p2.internal.ui.viewers.StructuredViewerProvisioningListener;
import ro.linic.ui.p2.org.eclipse.ui.PatternFilter;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Page that allows users to update, add, remove, import, and
 * export repositories.  This page can be hosted inside a preference
 * dialog or inside its own dialog.
 *
 * When hosting this page inside a non-preference dialog, some of the
 * dialog methods will likely have to call page methods.  The following
 * snippet shows how to host this page inside a TitleAreaDialog.
 * <pre>
 *		TitleAreaDialog dialog = new TitleAreaDialog(shell) {
 *
 *			RepositoryManipulationPage page;
 *
 *			protected Control createDialogArea(Composite parent) {
 *				page = new RepositoryManipulationPage();
 *              page.setProvisioningUI(ProvisioningUI.getDefaultUI());
 *				page.createControl(parent);
 *				this.setTitle("Software Sites");
 *				this.setMessage("The enabled sites will be searched for software.  Disabled sites are ignored.");
 *				return page.getControl();
 *			}
 *
 *			protected void okPressed() {
 *				if (page.performOk())
 *					super.okPressed();
 *			}
 *
 *			protected void cancelPressed() {
 *				if (page.performCancel())
 *					super.cancelPressed();
 *			}
 *		};
 *		dialog.open();
 * </pre>
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 2.0
 */
public class RepositoryManipulationPage extends PreferencePage implements IPreferencePage, ICopyable {
	final static String DEFAULT_FILTER_TEXT = ProvUIMessages.RepositoryManipulationPage_DefaultFilterString;
	private final static int FILTER_DELAY = 200;

	StructuredViewerProvisioningListener listener;
	CheckboxTableViewer repositoryViewer;
	Table table;
	ProvisioningUI ui;
	Policy policy;
	Display display;
	boolean changed = false;
	MetadataRepositoryElementComparator comparator;
	RepositoryDetailsLabelProvider labelProvider;
	RepositoryTracker tracker;
	RepositoryTracker localCacheRepoManipulator;
	/**
	 * The input field is initialized lazily and should only be accessed via the {@link #getInput()} method.
	 */
	CachedMetadataRepositories input;
	Text pattern, details;
	PatternFilter filter;
	Job filterJob;
	Button addButton, removeButton, editButton, refreshButton, disableButton;
//	Button exportButton;

	private Map<MetadataRepositoryElement, URI> originalURICache = new HashMap<>(2);
	private Map<MetadataRepositoryElement, String> originalNameCache = new HashMap<>(2);
	private IEclipseContext ctx;

	class CachedMetadataRepositories extends MetadataRepositories {
		private Hashtable<String, MetadataRepositoryElement> cachedElements;

		CachedMetadataRepositories(final IEclipseContext ctx) {
			super(ctx, ui);
			setIncludeDisabledRepositories(getPolicy().getRepositoriesVisible());
		}

		@Override
		public int getQueryType() {
			return QueryProvider.METADATA_REPOS;
		}

		@Override
		public Object[] fetchChildren(final Object o, final IProgressMonitor monitor) {
			if (cachedElements == null) {
				final Object[] children = super.fetchChildren(o, monitor);
				cachedElements = new Hashtable<>(children.length);
				for (final Object element : children) {
					if (element instanceof MetadataRepositoryElement) {
						put((MetadataRepositoryElement) element);
					}
				}
			}
			return cachedElements.values().toArray();
		}

		MetadataRepositoryElement[] getElements() {
			return cachedElements.values().toArray(new MetadataRepositoryElement[cachedElements.size()]);
		}

		void remove(final MetadataRepositoryElement element) {
			cachedElements.remove(getKey(element.getLocation()));
		}

		void put(final MetadataRepositoryElement element) {
			cachedElements.put(getKey(element.getLocation()), element);
		}

		MetadataRepositoryElement get(final URI location) {
			return cachedElements.get(getKey(location));
		}

		String getKey(final URI location) {
			String key = URIUtil.toUnencodedString(location);
			final int length = key.length();
			if (length > 0 && key.charAt(length - 1) == '/') {
				key = key.substring(0, length - 1);
			}
			return key;
		}

	}

	class MetadataRepositoryPatternFilter extends PatternFilter {
		MetadataRepositoryPatternFilter() {
			setIncludeLeadingWildcard(true);
		}

		@Override
		public boolean isElementVisible(final Viewer viewer, final Object element) {
			if (element instanceof MetadataRepositoryElement) {
				return wordMatches(labelProvider.getColumnText(element, RepositoryDetailsLabelProvider.COL_NAME) + " " + labelProvider.getColumnText(element, RepositoryDetailsLabelProvider.COL_LOCATION)); //$NON-NLS-1$
			}
			return false;
		}
	}

	/**
	 * Create a repository manipulation page that will display the repositories
	 * available to the user.
	 */
	public RepositoryManipulationPage(final IEclipseContext ctx) {
		this.ctx = ctx;
		this.setProvisioningUI(ProvisioningUI.getDefaultUI(ctx));
		noDefaultAndApplyButton();
	}

	/**
	 * Set the provisioning UI that provides the session, policy, and other
	 * services for the UI.  This method must be called before the contents are
	 * created or it will have no effect.
	 *
	 * @param ui the provisioning UI to use for this page.
	 */
	public void setProvisioningUI(final ProvisioningUI ui) {
		this.ui = ui;
		this.policy = ui.getPolicy();
		tracker = ui.getRepositoryTracker();
	}

	@Override
	protected Control createContents(final Composite parent) {
		display = parent.getDisplay();
		// The help refers to the full-blown dialog.  No help if it's read only.
//		if (policy.getRepositoriesVisible())
//			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(), IProvHelpContextIds.REPOSITORY_MANIPULATION_DIALOG);

		final Composite composite = new Composite(parent, SWT.NONE);

		// Filter box
		pattern = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.CANCEL);
		pattern.getAccessible().addAccessibleListener(AccessibleListener.getNameAdapter(e -> e.result = DEFAULT_FILTER_TEXT));
		pattern.setText(DEFAULT_FILTER_TEXT);
		pattern.selectAll();
		pattern.addModifyListener(e -> applyFilter());

		pattern.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.keyCode == SWT.ARROW_DOWN) {
				if (table.getItemCount() > 0) {
					table.setFocus();
				} else if (e.character == SWT.CR) {
					return;
				}
			}
		}));

		pattern.addFocusListener(FocusListener.focusGainedAdapter(e -> {
			display.asyncExec(() -> {
				if (!pattern.isDisposed()) {
					if (DEFAULT_FILTER_TEXT.equals(pattern.getText().trim())) {
						pattern.selectAll();
					}
				}
			});
		}));

		// spacer to fill other column
		if (policy.getRepositoriesVisible())
			new Label(composite, SWT.NONE);

		// Table of available repositories
		table = new Table(composite, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		repositoryViewer = new CheckboxTableViewer(table);

		// Key listener for delete
		table.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.keyCode == SWT.DEL) {
				removeRepositories();
			}
		}));
		setTableColumns();
//		CopyUtils.activateCopy(this, table);

		repositoryViewer.setComparer(new ProvElementComparer());
		comparator = new MetadataRepositoryElementComparator(RepositoryDetailsLabelProvider.COL_NAME);
		repositoryViewer.setComparator(comparator);
		filter = new MetadataRepositoryPatternFilter();
		repositoryViewer.setFilters(new ViewerFilter[] {filter});
		// We don't need a deferred content provider because we are caching local results before
		// actually querying
		repositoryViewer.setContentProvider(new ProvElementContentProvider());
		labelProvider = new RepositoryDetailsLabelProvider(ctx);
		repositoryViewer.setLabelProvider(labelProvider);

		// Edit the nickname
		repositoryViewer.setCellModifier(new ICellModifier() {
			@Override
			public boolean canModify(final Object element, final String property) {
				return element instanceof MetadataRepositoryElement;
			}

			@Override
			public Object getValue(final Object element, final String property) {
				return ((MetadataRepositoryElement) element).getName();
			}

			@Override
			public void modify(final Object element, final String property, final Object value) {
				if (value != null && value.toString().length() >= 0) {
					MetadataRepositoryElement repo;
					if (element instanceof Item) {
						repo = (MetadataRepositoryElement) ((Item) element).getData();
					} else if (element instanceof MetadataRepositoryElement) {
						repo = (MetadataRepositoryElement) element;
					} else {
						return;
					}
					if (!value.toString().equals(repo.getName())) {
						changed = true;
						repo.setNickname(value.toString());
						if (comparator.getSortKey() == RepositoryDetailsLabelProvider.COL_NAME)
							repositoryViewer.refresh(true);
						else
							repositoryViewer.update(repo, null);
					}
				}
			}

		});
		repositoryViewer.setColumnProperties(new String[] {"nickname"}); //$NON-NLS-1$
		repositoryViewer.setCellEditors(new CellEditor[] {new TextCellEditor(repositoryViewer.getTable())});

		repositoryViewer.addSelectionChangedListener(event -> {
			if (policy.getRepositoriesVisible())
				validateButtons();
			setDetails();
		});

		repositoryViewer.addDoubleClickListener(event -> {
			if (policy.getRepositoriesVisible())
				changeRepositoryProperties();
		});

		repositoryViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(final Object element) {
				return ((MetadataRepositoryElement) element).isEnabled();
			}

			@Override
			public boolean isGrayed(final Object element) {
				return false;
			}
		});

		repositoryViewer.addCheckStateListener(event -> {
			final MetadataRepositoryElement element = (MetadataRepositoryElement) event.getElement();
			element.setEnabled(event.getChecked());
			// paranoid that an equal but not identical element was passed in as the selection.
			// update the cache map just in case.
			getInput().put(element);
			// update the viewer to show the change
			updateForEnablementChange(new MetadataRepositoryElement[] {element});
		});

		// Input last
		repositoryViewer.setInput(getInput());

		// Drop targets and vertical buttons only if repository manipulation is provided.
		if (policy.getRepositoriesVisible()) {
			final DropTarget target = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
			target.addDropListener(new RepositoryManipulatorDropTarget(ctx, ui, table));

			// Vertical buttons
			final Composite verticalButtonBar = createVerticalButtonBar(composite);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(verticalButtonBar);
			listener = getViewerProvisioningListener();

			ProvUI.getProvisioningEventBus(ui.getSession()).addListener(listener);
			composite.addDisposeListener(event -> ProvUI.getProvisioningEventBus(ui.getSession()).removeListener(listener));

			validateButtons();
		}

		// Details area
		details = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_SITEDETAILS_HEIGHT);
		data.widthHint = 1;

		details.setLayoutData(data);

		GridLayoutFactory.fillDefaults().numColumns(policy.getRepositoriesVisible() ? 2 : 1).margins(LayoutConstants.getMargins()).spacing(LayoutConstants.getSpacing()).generateLayout(composite);

		Dialog.applyDialogFont(composite);
		return composite;
	}

	private Button createVerticalButton(final Composite parent, final String label, final boolean defaultButton) {
		final Button button = new Button(parent, SWT.PUSH);
		button.setText(label);

		final GridData data = setVerticalButtonLayoutData(button);
		data.horizontalAlignment = GridData.FILL;

		button.setToolTipText(label);
		if (defaultButton) {
			final Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		return button;
	}

	private GridData setVerticalButtonLayoutData(final Button button) {
		final GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		final int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		final Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		return data;
	}

	private void setTableColumns() {
		table.setHeaderVisible(true);
		String[] columnHeaders;
		if (policy.getRepositoriesVisible())
			columnHeaders = new String[] {ProvUIMessages.RepositoryManipulationPage_NameColumnTitle, ProvUIMessages.RepositoryManipulationPage_LocationColumnTitle, ProvUIMessages.RepositoryManipulationPage_EnabledColumnTitle};
		else
			columnHeaders = new String[] {ProvUIMessages.RepositoryManipulationPage_NameColumnTitle, ProvUIMessages.RepositoryManipulationPage_LocationColumnTitle};
		for (int i = 0; i < columnHeaders.length; i++) {
			final TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columnHeaders[i]);
			switch (i) {
				case RepositoryDetailsLabelProvider.COL_ENABLEMENT:
					tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH));
					tc.setAlignment(SWT.CENTER);
					break;
				case RepositoryDetailsLabelProvider.COL_NAME:
					tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH));
					break;
				default:
					tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH));
					break;
			}
			tc.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					columnSelected((TableColumn) e.widget);
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					columnSelected((TableColumn) e.widget);
				}

			});
			// First column only
			if (i == 0) {
				table.setSortColumn(tc);
				table.setSortDirection(SWT.UP);
			}
		}
	}

	private Composite createVerticalButtonBar(final Composite parent) {
		// Create composite.
		final Composite composite = new Composite(parent, SWT.NONE);
		initializeDialogUnits(composite);

		// create a layout with spacing and margins appropriate for the font
		// size.
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		createVerticalButtons(composite);
		return composite;
	}

	private void createVerticalButtons(final Composite parent) {
		addButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Add, false);
		addButton.addListener(SWT.Selection, event -> addRepository());

		editButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Edit, false);
		editButton.addListener(SWT.Selection, event -> changeRepositoryProperties());

		removeButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Remove, false);
		removeButton.addListener(SWT.Selection, event -> removeRepositories());

		refreshButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_RefreshConnection, false);
		refreshButton.addListener(SWT.Selection, event -> refreshRepository());

		disableButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_DisableButton, false);
		disableButton.addListener(SWT.Selection, event -> toggleRepositoryEnablement());

//		final Button button = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Import, false);
//		button.addListener(SWT.Selection, event -> importRepositories());
//
//		exportButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Export, false);
//		exportButton.addListener(SWT.Selection, event -> exportRepositories());
	}

	CachedMetadataRepositories getInput() {
		if (input == null)
			input = new CachedMetadataRepositories(ctx);
		return input;
	}

	@Override
	public boolean performOk() {
		if (changed)
			ElementUtils.updateRepositoryUsingElements(ui, getElements());
		originalNameCache.clear();
		originalURICache.clear();
		return super.performOk();
	}

	private StructuredViewerProvisioningListener getViewerProvisioningListener() {
		return new StructuredViewerProvisioningListener(ctx, RepositoryManipulationPage.this.getClass().getName(), repositoryViewer, ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, ui.getOperationRunner()) {
			@Override
			protected void repositoryDiscovered(final RepositoryEvent e) {
				RepositoryManipulationPage.this.safeRefresh(null);
			}

			@Override
			protected void repositoryChanged(final RepositoryEvent e) {
				RepositoryManipulationPage.this.safeRefresh(null);
			}
		};
	}

	MetadataRepositoryElement[] getElements() {
		return getInput().getElements();
	}

	MetadataRepositoryElement[] getSelectedElements() {
		final Object[] items = repositoryViewer.getStructuredSelection().toArray();
		final ArrayList<Object> list = new ArrayList<>(items.length);
		for (final Object item : items) {
			if (item instanceof MetadataRepositoryElement)
				list.add(item);
		}
		return list.toArray(new MetadataRepositoryElement[list.size()]);
	}

	void validateButtons() {
		final MetadataRepositoryElement[] elements = getSelectedElements();
//		exportButton.setEnabled(elements.length > 0);
		removeButton.setEnabled(elements.length > 0);
		editButton.setEnabled(elements.length == 1);
		refreshButton.setEnabled(elements.length == 1);
		if (elements.length >= 1) {
			if (toggleMeansDisable(elements))
				disableButton.setText(ProvUIMessages.RepositoryManipulationPage_DisableButton);
			else
				disableButton.setText(ProvUIMessages.RepositoryManipulationPage_EnableButton);
			disableButton.setEnabled(true);
		} else {
			disableButton.setText(ProvUIMessages.RepositoryManipulationPage_EnableButton);
			disableButton.setEnabled(false);
		}
	}

	void addRepository() {
		final AddRepositoryDialog dialog = new AddRepositoryDialog(getShell(), ui) {
			@Override
			protected RepositoryTracker getRepositoryTracker() {
				return RepositoryManipulationPage.this.getLocalCacheRepoTracker();
			}
		};
		dialog.setTitle(ProvUIMessages.ColocatedRepositoryManipulator_AddSiteOperationLabel);
		dialog.open();
	}

	void refreshRepository() {
		final MetadataRepositoryElement[] selected = getSelectedElements();
		final ProvisionException[] fail = new ProvisionException[1];
		final boolean[] remove = new boolean[1];
		remove[0] = false;
		if (selected.length != 1)
			return;
		final URI location = selected[0].getLocation();
		final ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, monitor -> {
				monitor.beginTask(NLS.bind(ProvUIMessages.RepositoryManipulationPage_ContactingSiteMessage, location), 100);
				try {
					// Batch the events for this operation so that any events on reload (discovery, etc.) will be ignored
					// in the UI as they happen.
					ui.signalRepositoryOperationStart();
					tracker.clearRepositoryNotFound(location);
					// If the managers don't know this repo, refreshing it will not work.
					// We temporarily add it, but we must remove it in case the user cancels out of this page.
					if (!includesRepo(tracker.getKnownRepositories(ui.getSession()), location)) {
						remove[0] = true;
						// We don't want to use the tracker here because it ensures that additions are
						// reported as user events to be responded to.  We don't want, for example, the
						// install wizard to change combo selections based on what is done here.
						ProvUI.getMetadataRepositoryManager(ui.getSession()).addRepository(location);
						ProvUI.getArtifactRepositoryManager(ui.getSession()).addRepository(location);
					}
					// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=312332
					// We assume repository colocation here.  Ideally we should not do this, but the
					// RepositoryTracker API is swallowing the refresh errors.
					final SubMonitor sub = SubMonitor.convert(monitor, 200);
					try {
						ProvUI.getMetadataRepositoryManager(ui.getSession()).refreshRepository(location, sub.newChild(100));
					} catch (final ProvisionException e1) {
						fail[0] = e1;
					}
					try {
						ProvUI.getArtifactRepositoryManager(ui.getSession()).refreshRepository(location, sub.newChild(100));
					} catch (final ProvisionException e2) {
						// Failure in the artifact repository.  We will not report this because the user has no separate visibility
						// of the artifact repository.  We should log the error.  If this repository fails during a download, the error
						// will be reported at that time to the user, when it matters.  This also prevents false error reporting when
						// a metadata repository didn't actually have a colocated artifact repository.
						LogHelper.log(e2);
					}
				} catch (final OperationCanceledException e3) {
					// Catch canceled login attempts
					fail[0] = new ProvisionException(new Status(IStatus.CANCEL, ProvUIAddon.PLUGIN_ID, ProvUIMessages.RepositoryManipulationPage_RefreshOperationCanceled, e3));
				} finally {
					// Check if the monitor was canceled
					if (fail[0] == null && monitor.isCanceled())
						fail[0] = new ProvisionException(new Status(IStatus.CANCEL, ProvUIAddon.PLUGIN_ID, ProvUIMessages.RepositoryManipulationPage_RefreshOperationCanceled));
					// If we temporarily added a repo so we could read it, remove it.
					if (remove[0]) {
						ProvUI.getMetadataRepositoryManager(ui.getSession()).removeRepository(location);
						ProvUI.getArtifactRepositoryManager(ui.getSession()).removeRepository(location);
					}
					ui.signalRepositoryOperationComplete(null, false);
				}
			});
		} catch (final InvocationTargetException e) {
			// nothing to report
		} catch (final InterruptedException e) {
			// nothing to report
		}
		if (fail[0] != null) {
			// If the repo was not found, tell ProvUI that we will be reporting it.
			// We are going to report problems directly to the status manager because we
			// do not want the automatic repo location editing to kick in.
			if (fail[0].getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
				tracker.addNotFound(location);
			}
			if (!fail[0].getStatus().matches(IStatus.CANCEL)) {
				// An error is only shown if the dialog was not canceled
				ProvUI.handleException(fail[0], null, StatusManager.SHOW);
			}
		} else {
			// Confirm that it was successful
			MessageDialog.openInformation(getShell(), ProvUIMessages.RepositoryManipulationPage_TestConnectionTitle, NLS.bind(ProvUIMessages.RepositoryManipulationPage_TestConnectionSuccess, URIUtil.toUnencodedString(location)));
		}
		repositoryViewer.update(selected[0], null);
		setDetails();
	}

	boolean includesRepo(final URI[] repos, final URI repo) {
		for (final URI repo2 : repos)
			if (repo2.equals(repo))
				return true;
		return false;
	}

	void toggleRepositoryEnablement() {
		final MetadataRepositoryElement[] selected = getSelectedElements();
		if (selected.length >= 1) {
			final boolean enableSites = !toggleMeansDisable(selected);
			for (final MetadataRepositoryElement select : selected) {
				select.setEnabled(enableSites);
				getInput().put(select);
			}
			updateForEnablementChange(selected);
		}
		validateButtons();
	}

	void updateForEnablementChange(final MetadataRepositoryElement[] updated) {
		if (comparator.getSortKey() == RepositoryDetailsLabelProvider.COL_ENABLEMENT)
			repositoryViewer.refresh(true);
		else
			for (final MetadataRepositoryElement element : updated)
				repositoryViewer.update(element, null);
		changed = true;
	}

//	void importRepositories() {
//		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
//			final MetadataRepositoryElement[] imported = UpdateManagerCompatibility.importSites(getShell());
//			if (imported.length > 0) {
//				changed = true;
//				for (final MetadataRepositoryElement element : imported)
//					getInput().put(element);
//				safeRefresh(null);
//			}
//		});
//	}
//
//	void exportRepositories() {
//		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
//			MetadataRepositoryElement[] elements = getSelectedElements();
//			if (elements.length == 0)
//				elements = getElements();
//			UpdateManagerCompatibility.exportSites(getShell(), elements);
//		});
//	}

	void changeRepositoryProperties() {
		final MetadataRepositoryElement[] selected = getSelectedElements();
		if (selected.length != 1)
			return;

		URI originalLocation = null;
		String originalName = null;
		if (originalURICache.containsKey(selected[0])) {
			originalLocation = originalURICache.get(selected[0]);
		} else
			originalLocation = selected[0].getLocation();
		if (originalNameCache.containsKey(selected[0])) {
			originalName = originalNameCache.get(selected[0]);
		} else
			originalName = selected[0].getName();
		final URI existingLocation = originalLocation;
		final RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(getShell(), ui) {
			@Override
			protected String getInitialLocationText() {
				return URIUtil.toUnencodedString(selected[0].getLocation());
			}

			@Override
			protected String getInitialNameText() {
				return selected[0].getName();
			}

			@Override
			protected URI getOriginalLocation() {
				return existingLocation;
			}
		};

		final int retCode = dialog.open();
		if (retCode == Window.OK && dialog.getLocation() != null) {
			selected[0].setNickname(dialog.getName());
			selected[0].setLocation(dialog.getLocation());
			if (dialog.getLocation().equals(existingLocation)) {
				// the change is reverted
				originalURICache.remove(selected[0]);
			} else if (!originalURICache.containsKey(selected[0]))
				originalURICache.put(selected[0], existingLocation);
			if (dialog.getName().equals(originalName)) {
				// the change is reverted
				originalNameCache.remove(selected[0]);
			} else if (!originalNameCache.containsKey(selected[0]))
				originalNameCache.put(selected[0], originalName);
			if (originalURICache.size() > 0 || originalNameCache.size() > 0)
				changed = true;
			else
				changed = false;
			repositoryViewer.update(selected[0], null);
			setDetails();
		}
	}

	void columnSelected(final TableColumn tc) {
		final TableColumn[] cols = table.getColumns();
		for (int i = 0; i < cols.length; i++) {
			if (cols[i] == tc) {
				if (i != comparator.getSortKey()) {
					comparator.setSortKey(i);
					table.setSortColumn(tc);
					comparator.sortAscending();
					table.setSortDirection(SWT.UP);
				} else {
					if (comparator.isAscending()) {
						table.setSortDirection(SWT.DOWN);
						comparator.sortDescending();
					} else {
						table.setSortDirection(SWT.UP);
						comparator.sortAscending();
					}
				}
				repositoryViewer.refresh();
				break;
			}
		}
	}

	void safeRefresh(final MetadataRepositoryElement elementToSelect) {
		final Runnable runnable = () -> {
			repositoryViewer.refresh();
			if (elementToSelect != null)
				repositoryViewer.setSelection(new StructuredSelection(elementToSelect), true);
		};
		if (Display.getCurrent() == null)
			display.asyncExec(runnable);
		else
			runnable.run();
	}

	void applyFilter() {
		String text = pattern.getText();
		if (text == DEFAULT_FILTER_TEXT)
			text = ""; //$NON-NLS-1$
		if (text.length() == 0)
			filter.setPattern(null);
		else
			filter.setPattern(text);
		if (filterJob != null)
			filterJob.cancel();
		filterJob = org.eclipse.e4.ui.progress.UIJob.create("filter job", monitor -> { //$NON-NLS-1$
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			if (!repositoryViewer.getTable().isDisposed())
				repositoryViewer.refresh();
			return Status.OK_STATUS;
		});
		filterJob.setSystem(true);
		filterJob.schedule(FILTER_DELAY);
	}

	void setDetails() {
		final MetadataRepositoryElement[] selections = getSelectedElements();
		if (selections.length == 1) {
			details.setText(selections[0].getDescription());
		} else {
			details.setText(""); //$NON-NLS-1$
		}
	}

	void removeRepositories() {
		final MetadataRepositoryElement[] selections = getSelectedElements();
		if (selections.length > 0) {
			String message = ProvUIMessages.RepositoryManipulationPage_RemoveConfirmMessage;
			if (selections.length == 1)
				message = NLS.bind(ProvUIMessages.RepositoryManipulationPage_RemoveConfirmSingleMessage, URIUtil.toUnencodedString(selections[0].getLocation()));
			if (MessageDialog.openQuestion(getShell(), ProvUIMessages.RepositoryManipulationPage_RemoveConfirmTitle, message)) {

				changed = true;
				for (final MetadataRepositoryElement selection : selections) {
					getInput().remove(selection);
				}
				safeRefresh(null);
			}
		}
	}

	// Return a repo manipulator that only operates on the local cache.
	// Labels and other presentation info are used from the original manipulator.
	RepositoryTracker getLocalCacheRepoTracker() {
		if (localCacheRepoManipulator == null)
			localCacheRepoManipulator = new RepositoryTracker() {
				@Override
				public void addRepository(final URI location, final String nickname, final ProvisioningSession session) {
					MetadataRepositoryElement element = getInput().get(location);
					if (element == null) {
						element = new MetadataRepositoryElement(ctx, getInput(), null, ui, location, true);
						getInput().put(element);
					}
					if (nickname != null)
						element.setNickname(nickname);
					changed = true;
					safeRefresh(element);
				}

				@Override
				public URI[] getKnownRepositories(final ProvisioningSession session) {
					return RepositoryManipulationPage.this.getKnownRepositories();
				}

				@Override
				public void removeRepositories(final URI[] repoLocations, final ProvisioningSession session) {
					RepositoryManipulationPage.this.removeRepositories();
				}

				@Override
				public void refreshRepositories(final URI[] locations, final ProvisioningSession session, final IProgressMonitor monitor) {
					// Nothing to refresh in the local cache
				}

				@Override
				public IStatus validateRepositoryLocation(final ProvisioningSession session, URI location, final boolean contactRepositories, final IProgressMonitor monitor) {
					IStatus status = super.validateRepositoryLocation(session, location, contactRepositories, monitor);
					if (status.isOK()) {
						final String repoString = URIUtil.toUnencodedString(location);
						final int length = repoString.length();
						if (length > 0 && repoString.charAt(length - 1) == '/') {
							try {
								location = URIUtil.fromString(repoString.substring(0, length - 1));
							} catch (final URISyntaxException e) {
								return status;
							}
							status = super.validateRepositoryLocation(session, location, contactRepositories, monitor);
						}
					}
					return status;

				}
			};
		return localCacheRepoManipulator;
	}

	@Override
	public void copyToClipboard(final Control activeControl) {
		MetadataRepositoryElement[] elements = getSelectedElements();
		if (elements.length == 0)
			elements = getElements();
		String text = ""; //$NON-NLS-1$
		final StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			buffer.append(labelProvider.getClipboardText(elements[i], CopyUtils.DELIMITER));
			if (i > 0)
				buffer.append(CopyUtils.NEWLINE);
		}
		text = buffer.toString();

		if (text.length() == 0)
			return;
		final Clipboard clipboard = new Clipboard(Display.getCurrent());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	// If more than half of the selected repos are enabled, toggle means disable.
	// Otherwise it means enable.
	private boolean toggleMeansDisable(final MetadataRepositoryElement[] elements) {
		double count = 0;
		for (final MetadataRepositoryElement element : elements) {
			if (element.isEnabled()) {
				count++;
			}
		}
		return (count / elements.length) > 0.5;
	}

	URI[] getKnownRepositories() {
		final MetadataRepositoryElement[] elements = getElements();
		final URI[] locations = new URI[elements.length];
		for (int i = 0; i < elements.length; i++)
			locations[i] = elements[i].getLocation();
		return locations;
	}
}
