package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.ArrayList;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.AvailableUpdateElement;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.model.IUElementListRoot;
import ro.linic.ui.p2.internal.ui.viewers.IUColumnConfig;
import ro.linic.ui.p2.internal.ui.viewers.IUComparator;
import ro.linic.ui.p2.internal.ui.viewers.IUDetailsLabelProvider;
import ro.linic.ui.p2.internal.ui.viewers.ProvElementComparer;
import ro.linic.ui.p2.internal.ui.viewers.ProvElementContentProvider;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * A wizard page that presents a check box list of IUs and allows the user to
 * select and deselect them. Typically the first page in a provisioning
 * operation wizard, and usually it is the page used to report resolution errors
 * before advancing to resolution detail.
 *
 * @since 3.5
 *
 */
public class SelectableIUsPage extends ResolutionStatusPage implements IResolutionErrorReportingPage {

	private static final String DIALOG_SETTINGS_SECTION = "SelectableIUsPage"; //$NON-NLS-1$

	IUElementListRoot root;
	Object[] initialSelections;
	ProfileChangeOperation resolvedOperation;
	CheckboxTableViewer tableViewer;
	IUDetailsGroup iuDetailsGroup;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	protected Policy policy;
	SashForm sashForm;

	public SelectableIUsPage(final IEclipseContext ctx, final ProvisioningUI ui, final ProvisioningOperationWizard wizard, IUElementListRoot root,
			final Object[] initialSelections) {
		super(ctx, "IUSelectionPage", ui, wizard); //$NON-NLS-1$
		this.root = root;
		if (root == null)
			root = new IUElementListRoot(ctx, ui);
		if (initialSelections == null)
			this.initialSelections = new IInstallableUnit[0];
		else
			this.initialSelections = initialSelections;

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

		tableViewer = createTableViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		final Table table = tableViewer.getTable();
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		activateCopy(table);
		final IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			final TableColumn tc = new TableColumn(table, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(table));
		}

		tableViewer.addSelectionChangedListener(event -> setDetailText(resolvedOperation));

		tableViewer.addCheckStateListener(event -> {
			// If the checkEvent is on a locked update element, uncheck it and select it.
			if (event.getElement() instanceof AvailableUpdateElement) {
				final AvailableUpdateElement checkedElement = (AvailableUpdateElement) event.getElement();
				if (checkedElement.isLockedForUpdate()) {
					event.getCheckable().setChecked(checkedElement, false);
					// Select the element so that the locked description is displayed
					final CheckboxTableViewer viewer = ((CheckboxTableViewer) event.getSource());
					final int itemCount = viewer.getTable().getItemCount();
					for (int i = 0; i < itemCount; i++) {
						if (viewer.getElementAt(i).equals(checkedElement)) {
							viewer.getTable().deselectAll();
							viewer.getTable().select(i);
							setDetailText(resolvedOperation);
							break;
						}
					}
				}
			}
			updateSelection();
		});

		// Filters and sorters before establishing content, so we don't refresh
		// unnecessarily.
		final IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(ProvUI.getIUColumnConfig());
		tableViewer.setComparator(comparator);
		tableViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		tableViewer.setContentProvider(contentProvider);
		labelProvider = new IUDetailsLabelProvider(ctx, null, ProvUI.getIUColumnConfig(), getShell());
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(root);
		setInitialCheckState();

		// Select and Deselect All buttons
		createSelectButtons(composite);

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

		// The text area shows a description of the selected IU, or error detail if
		// applicable.
		iuDetailsGroup = new IUDetailsGroup(sashForm, tableViewer,
				convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

		updateStatus(root, resolvedOperation);
		setControl(sashForm);
		sashForm.setWeights(getSashWeights());
		Dialog.applyDialogFont(sashForm);
	}

	private void createSelectButtons(final Composite parent) {
		final Composite buttonParent = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonParent.setLayout(gridLayout);
		GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		buttonParent.setLayoutData(data);

		final Button selectAll = new Button(buttonParent, SWT.PUSH);
		selectAll.setText(ProvUIMessages.SelectableIUsPage_Select_All);
		setButtonLayoutData(selectAll);
		selectAll.addListener(SWT.Selection, event -> {
			tableViewer.setAllChecked(true);
			updateSelection();
		});

		final Button deselectAll = new Button(buttonParent, SWT.PUSH);
		deselectAll.setText(ProvUIMessages.SelectableIUsPage_Deselect_All);
		setButtonLayoutData(deselectAll);
		deselectAll.addListener(SWT.Selection, event -> {
			tableViewer.setAllChecked(false);
			updateSelection();
		});

		// dummy to take extra space
		final Label dummy = new Label(buttonParent, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		dummy.setLayoutData(data);

		// separator underneath
		final Label sep = new Label(buttonParent, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		data.horizontalSpan = 3;
		sep.setLayoutData(data);
	}

	protected CheckboxTableViewer createTableViewer(final Composite parent) {
		// The viewer allows selection of IU's for browsing the details,
		// and checking to include in the provisioning operation.
		final CheckboxTableViewer v = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		return v;
	}

	@Override
	public Object[] getCheckedIUElements() {
		if (tableViewer == null)
			return initialSelections;
		return tableViewer.getCheckedElements();
	}

	@Override
	public Object[] getSelectedIUElements() {
		return tableViewer.getStructuredSelection().toArray();
	}

	@Override
	protected Object[] getSelectedElements() {
		return tableViewer.getStructuredSelection().toArray();
	}

	protected IInstallableUnit[] elementsToIUs(final Object[] elements) {
		final IInstallableUnit[] theIUs = new IInstallableUnit[elements.length];
		for (int i = 0; i < elements.length; i++) {
			theIUs[i] = ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		}
		return theIUs;
	}

	protected void setInitialCheckState() {
		if (initialSelections == null) {
			return;
		}

		final ArrayList<Object> selections = new ArrayList<>(initialSelections.length);

		for (final Object initialSelection : initialSelections) {
			if (initialSelection instanceof AvailableUpdateElement) {
				final AvailableUpdateElement element = (AvailableUpdateElement) initialSelection;
				if (element.isLockedForUpdate()) {
					continue;
				}
			}
			selections.add(initialSelection);
		}
		tableViewer.setCheckedElements(selections.toArray(new Object[selections.size()]));
	}

	/*
	 * Overridden so that we don't call getNextPage(). We use getNextPage() to start
	 * resolving the operation so we only want to do that when the next button is
	 * pressed.
	 *
	 */
	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	/*
	 * Overridden to null out any cached page so that the wizard is always
	 * consulted. This allows wizards to do things like synchronize previous page
	 * selections with this page.
	 */
	@Override
	public IWizardPage getPreviousPage() {
		setPreviousPage(null);
		return super.getPreviousPage();
	}

	@Override
	protected String getClipboardText(final Control control) {
		final StringBuilder buffer = new StringBuilder();
		final Object[] elements = getSelectedElements();
		for (int i = 0; i < elements.length; i++) {
			if (i > 0)
				buffer.append(CopyUtils.NEWLINE);
			buffer.append(labelProvider.getClipboardText(elements[i], CopyUtils.DELIMITER));
		}
		return buffer.toString();
	}

	@Override
	protected IInstallableUnit getSelectedIU() {
		final java.util.List<IInstallableUnit> units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.size() == 0)
			return null;
		return units.get(0);
	}

	@Override
	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	@Override
	protected boolean isCreated() {
		return tableViewer != null;
	}

	@Override
	protected void updateCaches(final IUElementListRoot newRoot, final ProfileChangeOperation op) {
		resolvedOperation = op;
		if (newRoot != null && root != newRoot) {
			root = newRoot;
			if (tableViewer != null)
				tableViewer.setInput(newRoot);
		}

	}

	@Override
	public void setCheckedElements(final Object[] elements) {
		if (tableViewer == null)
			initialSelections = elements;
		else
			tableViewer.setCheckedElements(elements);
	}

	@Override
	protected SashForm getSashForm() {
		return sashForm;
	}

	@Override
	protected String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	@Override
	protected int getColumnWidth(final int index) {
		return tableViewer.getTable().getColumn(index).getWidth();
	}

	void updateSelection() {
		setPageComplete(tableViewer.getCheckedElements().length > 0);
		getProvisioningWizard().operationSelectionsChanged(this);
	}
}
