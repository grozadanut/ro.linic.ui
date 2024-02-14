package ro.linic.ui.p2.internal.ui.dialogs;

import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.internal.ui.viewers.IUColumnConfig;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * A StructuredIUGroup is a reusable UI component that displays a
 * structured view of IU's driven by some queries.
 *
 * @since 3.4
 */
public abstract class StructuredIUGroup {

	private FontMetrics fm;
	protected StructuredViewer viewer;
	private Composite composite;
	private ProvisioningUI ui;
	private IUColumnConfig[] columnConfig;

	/**
	 * Create a group that represents the available IU's.
	 *
	 * @param ui The application policy to use in the group
	 * @param parent the parent composite for the group
	 * to retrieve elements in the viewer.
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param columnConfig the columns to be shown
	 */
	protected StructuredIUGroup(final ProvisioningUI ui, final Composite parent, final Font font, final IUColumnConfig[] columnConfig) {
		this.ui = ui;
		if (columnConfig == null)
			this.columnConfig = ProvUI.getIUColumnConfig();
		else
			this.columnConfig = columnConfig;

		// Set up a fontmetrics for calculations
		final GC gc = new GC(parent);
		gc.setFont(font);
		fm = gc.getFontMetrics();
		gc.dispose();
	}

	protected void createGroupComposite(final Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());

		viewer = createViewer(composite);
		viewer.getControl().setLayoutData(getViewerGridData());
	}

	protected GridData getViewerGridData() {
		final GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		return data;
	}

	protected abstract StructuredViewer createViewer(Composite parent);

	protected Composite getComposite() {
		return composite;
	}

	protected Shell getShell() {
		return composite.getShell();
	}

	protected StructuredViewer getStructuredViewer() {
		return viewer;
	}

	protected IUColumnConfig[] getColumnConfig() {
		return columnConfig;
	}

	public List<IInstallableUnit> getSelectedIUs() {
		return ElementUtils.elementsToIUs(getSelectedIUElements());
	}

	public Object[] getSelectedIUElements() {
		return viewer.getStructuredSelection().toArray();
	}

	protected int convertHorizontalDLUsToPixels(final int dlus) {
		return Dialog.convertHorizontalDLUsToPixels(fm, dlus);
	}

	protected int convertWidthInCharsToPixels(final int dlus) {
		return Dialog.convertWidthInCharsToPixels(fm, dlus);
	}

	protected int convertVerticalDLUsToPixels(final int dlus) {
		return Dialog.convertVerticalDLUsToPixels(fm, dlus);
	}

	protected int convertHeightInCharsToPixels(final int dlus) {
		return Dialog.convertHeightInCharsToPixels(fm, dlus);
	}

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	protected ProvisioningSession getSession() {
		return ui.getSession();
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}

	protected Control getDefaultFocusControl() {
		if (viewer != null)
			return viewer.getControl();
		return null;
	}
}
