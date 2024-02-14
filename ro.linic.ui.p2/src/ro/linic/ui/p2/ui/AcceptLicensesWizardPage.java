package ro.linic.ui.p2.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.dialogs.ILayoutConstants;
import ro.linic.ui.p2.internal.ui.viewers.IUColumnConfig;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * AcceptLicensesWizardPage shows a list of the IU's that have
 * licenses that have not been approved by the user, and allows the
 * user to approve them.
 *
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class AcceptLicensesWizardPage extends WizardPage {
	private static final String DIALOG_SETTINGS_SECTION = "LicensessPage"; //$NON-NLS-1$
	private static final String LIST_WEIGHT = "ListSashWeight"; //$NON-NLS-1$
	private static final String LICENSE_WEIGHT = "LicenseSashWeight"; //$NON-NLS-1$
	private static final String NAME_COLUMN_WIDTH = "NameColumnWidth"; //$NON-NLS-1$
	private static final String VERSION_COLUMN_WIDTH = "VersionColumnWidth"; //$NON-NLS-1$

	class IUWithLicenseParent {
		IInstallableUnit iu;
		ILicense license;

		IUWithLicenseParent(final ILicense license, final IInstallableUnit iu) {
			this.license = license;
			this.iu = iu;
		}
	}

	class LicenseContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(final Object parentElement) {
			if (!(parentElement instanceof ILicense))
				return new Object[0];

			if (licensesToIUs.containsKey(parentElement)) {
				final List<IInstallableUnit> iusWithLicense = licensesToIUs.get(parentElement);
				final IInstallableUnit[] ius = iusWithLicense.toArray(new IInstallableUnit[iusWithLicense.size()]);
				final IUWithLicenseParent[] children = new IUWithLicenseParent[ius.length];
				for (int i = 0; i < ius.length; i++) {
					children[i] = new IUWithLicenseParent((ILicense) parentElement, ius[i]);
				}
				return children;
			}
			return null;
		}

		@Override
		public Object getParent(final Object element) {
			if (element instanceof IUWithLicenseParent) {
				return ((IUWithLicenseParent) element).license;
			}
			return null;
		}

		@Override
		public boolean hasChildren(final Object element) {
			return licensesToIUs.containsKey(element);
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return licensesToIUs.keySet().toArray();
		}

		@Override
		public void dispose() {
			// Nothing to do
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// Nothing to do
		}
	}

	class LicenseLabelProvider extends LabelProvider {
		@Override
		public Image getImage(final Object element) {
			return null;
		}

		@Override
		public String getText(final Object element) {
			if (element instanceof License) {
				return getFirstLine(((License) element).getBody());
			} else if (element instanceof IUWithLicenseParent) {
				return getIUName(((IUWithLicenseParent) element).iu);
			} else if (element instanceof IInstallableUnit) {
				return getIUName((IInstallableUnit) element);
			}
			return ""; //$NON-NLS-1$
		}

		private String getFirstLine(final String body) {
			final int i = body.indexOf('\n');
			final int j = body.indexOf('\r');
			if (i > 0) {
				if (j > 0)
					return body.substring(0, i < j ? i : j);
				return body.substring(0, i);
			} else if (j > 0) {
				return body.substring(0, j);
			}
			return body;
		}
	}

	TreeViewer iuViewer;
	Text licenseTextBox;
	Button acceptButton;
	Button declineButton;
	SashForm sashForm;
	private IInstallableUnit[] originalIUs;
	HashMap<ILicense, List<IInstallableUnit>> licensesToIUs; // License -> IU Name
	private LicenseManager manager;
	IUColumnConfig nameColumn;
	IUColumnConfig versionColumn;
	protected IEclipseContext ctx;

	static String getIUName(final IInstallableUnit iu) {
		final StringBuilder buf = new StringBuilder();
		final String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		if (name != null)
			buf.append(name);
		else
			buf.append(iu.getId());
		buf.append(" "); //$NON-NLS-1$
		buf.append(iu.getVersion().toString());
		return buf.toString();
	}

	/**
	 * Create a license acceptance page for showing licenses to the user.
	 *
	 * @param manager the license manager that should be used to check for already accepted licenses.  May be <code>null</code>.
	 * @param ius the IInstallableUnits for which licenses should be checked
	 * @param operation the provisioning operation describing what changes are to take place on the profile
	 */
	public AcceptLicensesWizardPage(final IEclipseContext ctx, final LicenseManager manager, final IInstallableUnit[] ius,
			final ProfileChangeOperation operation) {
		super("AcceptLicenses"); //$NON-NLS-1$
		this.ctx = ctx;
		setTitle(ProvUIMessages.AcceptLicensesWizardPage_Title);
		this.manager = manager;
		update(ius, operation);
	}

	@Override
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);
		List<IInstallableUnit> ius;
		if (licensesToIUs == null || licensesToIUs.size() == 0) {
			final Label label = new Label(parent, SWT.NONE);
			setControl(label);
		} else if (licensesToIUs.size() == 1 && (ius = licensesToIUs.values().iterator().next()).size() == 1) {
			createLicenseContentSection(parent, ius.get(0));
		} else {
			sashForm = new SashForm(parent, SWT.HORIZONTAL);
			sashForm.setLayout(new GridLayout());
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			sashForm.setLayoutData(gd);

			createLicenseListSection(sashForm);
			createLicenseContentSection(sashForm, null);
			sashForm.setWeights(getSashWeights());
			setControl(sashForm);
		}
		Dialog.applyDialogFont(getControl());
	}

	private void createLicenseListSection(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		final Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.AcceptLicensesWizardPage_ItemsLabel);
		iuViewer = new TreeViewer(composite, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		iuViewer.setContentProvider(new LicenseContentProvider());
		iuViewer.setLabelProvider(new LicenseLabelProvider());
		iuViewer.setComparator(new ViewerComparator());
		iuViewer.setInput(licensesToIUs);

		iuViewer.addSelectionChangedListener(event -> handleSelectionChanged(event.getStructuredSelection()));
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		iuViewer.getControl().setLayoutData(gd);
	}

	private void createLicenseAcceptSection(final Composite parent, final boolean multiple) {
		// Buttons for accepting licenses
		final Composite buttonContainer = new Composite(parent, SWT.NULL);
		final GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		buttonContainer.setLayout(new GridLayout());
		buttonContainer.setLayoutData(gd);

		acceptButton = new Button(buttonContainer, SWT.RADIO);
		if (multiple)
			acceptButton.setText(ProvUIMessages.AcceptLicensesWizardPage_AcceptMultiple);
		else
			acceptButton.setText(ProvUIMessages.AcceptLicensesWizardPage_AcceptSingle);

		acceptButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setPageComplete(acceptButton.getSelection())));
		declineButton = new Button(buttonContainer, SWT.RADIO);
		if (multiple)
			declineButton.setText(ProvUIMessages.AcceptLicensesWizardPage_RejectMultiple);
		else
			declineButton.setText(ProvUIMessages.AcceptLicensesWizardPage_RejectSingle);
		declineButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setPageComplete(!declineButton.getSelection())));

		acceptButton.setSelection(false);
		declineButton.setSelection(true);
	}

	private void createLicenseContentSection(final Composite parent, final IInstallableUnit singleIU) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		final Label label = new Label(composite, SWT.NONE);
		if (singleIU == null)
			label.setText(ProvUIMessages.AcceptLicensesWizardPage_LicenseTextLabel);
		else
			label.setText(NLS.bind(ProvUIMessages.AcceptLicensesWizardPage_SingleLicenseTextLabel, getIUName(singleIU)));
		licenseTextBox = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		licenseTextBox.setBackground(licenseTextBox.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		initializeDialogUnits(licenseTextBox);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		gd.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH);
		licenseTextBox.setLayoutData(gd);

		createLicenseAcceptSection(composite, licensesToIUs.size() > 1);

		if (singleIU != null) {
			String licenseBody = ""; //$NON-NLS-1$
			// We've already established before calling this method that it's a single IU with a single license
			final Iterator<ILicense> licenses = singleIU.getLicenses(null).iterator();
			final ILicense license = licenses.hasNext() ? licenses.next() : null;
			if (license != null && license.getBody() != null) {
				licenseBody = license.getBody();
			}
			licenseTextBox.setText(licenseBody);
		}
		setControl(composite);
	}

	void handleSelectionChanged(final IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			final Object selected = selection.getFirstElement();
			if (selected instanceof License)
				licenseTextBox.setText(((License) selected).getBody());
			else if (selected instanceof IUWithLicenseParent)
				licenseTextBox.setText(((IUWithLicenseParent) selected).license.getBody());
		}
	}

	/**
	 * The wizard is finishing.  Perform any necessary processing.
	 *
	 * @return <code>true</code> if the finish can proceed,
	 * <code>false</code> if it should not.
	 */
	public boolean performFinish() {
		rememberAcceptedLicenses();
		return true;
	}

	/**
	 * Return a boolean indicating whether there are licenses that must be accepted
	 * by the user.
	 *
	 * @return <code>true</code> if there are licenses that must be accepted, and
	 * <code>false</code> if there are no licenses that must be accepted.
	 */
	public boolean hasLicensesToAccept() {
		return licensesToIUs != null && licensesToIUs.size() > 0;
	}

	/**
	 * Update the current page to show the licenses that must be approved for the
	 * selected IUs and the provisioning plan.
	 *
	 * Clients using this page in conjunction with a {@link ProfileChangeOperation} should
	 * instead use {@link #update(IInstallableUnit[], ProfileChangeOperation)}.   This
	 * method is intended for clients who are working with a low-level provisioning plan
	 * rather than an {@link InstallOperation} or {@link UpdateOperation}.
	 *
	 * @param theIUs the installable units to be installed for which licenses must be checked
	 * @param plan the provisioning plan that describes a resolved install operation
	 *
	 * @see #update(IInstallableUnit[], ProfileChangeOperation)
	 */

	public void updateForPlan(final IInstallableUnit[] theIUs, final IProvisioningPlan plan) {
		updateLicenses(theIUs, plan);
	}

	private void updateLicenses(final IInstallableUnit[] theIUs, final IProvisioningPlan plan) {
		this.originalIUs = theIUs;
		if (theIUs == null)
			licensesToIUs = new HashMap<>();
		else
			findUnacceptedLicenses(theIUs, plan);
		setDescription();
		setPageComplete(licensesToIUs.size() == 0);
		if (getControl() != null) {
			final Composite parent = getControl().getParent();
			getControl().dispose();
			iuViewer = null;
			sashForm = null;
			createControl(parent);
			parent.layout(true);
		}
	}

	/**
	 * Update the page for the specified IInstallableUnits and operation.
	 *
	 * @param theIUs the IInstallableUnits for which licenses should be checked
	 * @param operation the operation describing the pending profile change
	 */
	public void update(final IInstallableUnit[] theIUs, final ProfileChangeOperation operation) {
		if (operation != null && operation.hasResolved()) {
			final int sev = operation.getResolutionResult().getSeverity();
			if (sev != IStatus.ERROR && sev != IStatus.CANCEL) {
				updateLicenses(theIUs, operation.getProvisioningPlan());
			} else {
				updateLicenses(new IInstallableUnit[0], null);
			}
		}
	}

	private void findUnacceptedLicenses(final IInstallableUnit[] selectedIUs, final IProvisioningPlan plan) {
		IInstallableUnit[] iusToCheck = selectedIUs;
		if (plan != null) {
			iusToCheck = plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null).toArray(IInstallableUnit.class);
		}

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=218532
		// Current metadata generation can result with a feature group IU and the feature jar IU
		// having the same name and license.  We will weed out duplicates if the license and name are both
		// the same.
		licensesToIUs = new HashMap<>();//map of License->ArrayList of IUs with that license
		final HashMap<ILicense, HashSet<String>> namesSeen = new HashMap<>(); // map of License->HashSet of names with that license
		for (final IInstallableUnit iu : iusToCheck) {
			for (final ILicense license : iu.getLicenses(null)) {
				if (manager != null && !manager.isAccepted(license)) {
					String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
					if (name == null)
						name = iu.getId();
					// Have we already found this license?
					if (licensesToIUs.containsKey(license)) {
						final HashSet<String> names = namesSeen.get(license);
						if (!names.contains(name)) {
							names.add(name);
							((ArrayList<IInstallableUnit>) licensesToIUs.get(license)).add(iu);
						}
					} else {
						final ArrayList<IInstallableUnit> list = new ArrayList<>(1);
						list.add(iu);
						licensesToIUs.put(license, list);
						final HashSet<String> names = new HashSet<>(1);
						names.add(name);
						namesSeen.put(license, names);
					}
				}
			}
		}
	}

	private void rememberAcceptedLicenses() {
		if (licensesToIUs == null || manager == null)
			return;
		for (final ILicense license : licensesToIUs.keySet())
			manager.accept(license);
	}

	private void setDescription() {
		// No licenses but the page is open.  Shouldn't happen, but just in case...
		if (licensesToIUs == null || licensesToIUs.size() == 0)
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_NoLicensesDescription);
		// We have licenses.  Use a generic message if we think we aren't showing extra
		// licenses from required IU's.  This check is not entirely accurate, for example
		// one root IU could have no license and the next one has two different
		// IU's with different licenses.  But this cheaply catches the common cases.
		else if (licensesToIUs.size() <= originalIUs.length)
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_ReviewLicensesDescription);
		else {
			// Without a doubt we know we are showing extra licenses.
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_ReviewExtraLicensesDescription);
		}
	}

	private String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	/**
	 * Save any settings related to the current size and location of the wizard page.
	 */
	public void saveBoundsRelatedSettings() {
		if (iuViewer == null || iuViewer.getTree().isDisposed())
			return;
		final Preferences settings = InstanceScope.INSTANCE.getNode(ProvUIAddon.PLUGIN_ID);
		final Preferences section = settings.node(getDialogSettingsName());
		section.putInt(NAME_COLUMN_WIDTH, iuViewer.getTree().getColumn(0).getWidth());
		section.putInt(VERSION_COLUMN_WIDTH, iuViewer.getTree().getColumn(1).getWidth());

		if (sashForm == null || sashForm.isDisposed())
			return;
		final int[] weights = sashForm.getWeights();
		section.putInt(LIST_WEIGHT, weights[0]);
		section.putInt(LICENSE_WEIGHT, weights[1]);
		try {
			// forces the application to save the preferences
			settings.flush();
		} catch (final BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private int[] getSashWeights() {
		final Preferences settings = InstanceScope.INSTANCE.getNode(ProvUIAddon.PLUGIN_ID);
		final Preferences section = settings.node(getDialogSettingsName());
		if (section != null) {
			try {
				final int[] weights = new int[2];
				if (section.get(LIST_WEIGHT, null) != null) {
					weights[0] = section.getInt(LIST_WEIGHT, 55);
					if (section.get(LICENSE_WEIGHT, null) != null) {
						weights[1] = section.getInt(LICENSE_WEIGHT, 45);
						return weights;
					}
				}
			} catch (final NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.
			}
		}
		return new int[] {55, 45};
	}

	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible && hasLicensesToAccept() && iuViewer != null) {
			iuViewer.setSelection(new StructuredSelection(iuViewer.getTree().getItem(0).getData()), true);
		}
	}
}
