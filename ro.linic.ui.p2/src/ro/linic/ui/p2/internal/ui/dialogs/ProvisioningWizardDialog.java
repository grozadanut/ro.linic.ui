package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * Subclass of WizardDialog that provides bounds saving behavior.
 * @since 3.5
 *
 */
public class ProvisioningWizardDialog extends WizardDialog {
	private ProvisioningOperationWizard wizard;
	private IEclipseContext ctx;

	public ProvisioningWizardDialog(final IEclipseContext ctx, final Shell parent, final ProvisioningOperationWizard wizard) {
		super(parent, wizard);
		this.ctx = ctx;
		this.wizard = wizard;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

//	@Override
//	protected IDialogSettings getDialogBoundsSettings() {
//		final IDialogSettings settings = ctx.get(IDialogSettings.class);
//		IDialogSettings section = settings.getSection(wizard.getDialogSettingsSectionName());
//		if (section == null) {
//			section = settings.addNewSection(wizard.getDialogSettingsSectionName());
//		}
//		return section;
//	}

	/**
	 * @see org.eclipse.jface.window.Window#close()
	 */
	@Override
	public boolean close() {
		if (getShell() != null && !getShell().isDisposed()) {
			wizard.saveBoundsRelatedSettings();
		}
		return super.close();
	}

	/**
	 * This method is provided only for automated testing.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Button testGetButton(final int id) {
		return getButton(id);
	}
}
