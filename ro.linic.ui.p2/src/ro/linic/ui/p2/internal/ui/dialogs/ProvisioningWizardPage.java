package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import ro.linic.ui.p2.ui.ICopyable;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

abstract class ProvisioningWizardPage extends WizardPage implements ICopyable {

	private ProvisioningUI ui;
	private ProvisioningOperationWizard wizard;

	protected ProvisioningWizardPage(final String pageName, final ProvisioningUI ui, final ProvisioningOperationWizard wizard) {
		super(pageName);
		this.wizard = wizard;
		this.ui = ui;
	}

	protected void activateCopy(final Control control) {
//		CopyUtils.activateCopy(this, control);
	}

	protected ProvisioningOperationWizard getProvisioningWizard() {
		return wizard;
	}

	@Override
	public void copyToClipboard(final Control activeControl) {
		final String text = getClipboardText(activeControl);
		if (text.length() == 0)
			return;
		final Clipboard clipboard = new Clipboard(Display.getCurrent());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	protected abstract String getClipboardText(Control control);

	/**
	 * Save any settings that are related to the bounds of the wizard.
	 * This method is called when the wizard is about to close.
	 */
	public void saveBoundsRelatedSettings() {
		// Default is to do nothing
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	String getProfileId() {
		return ui.getProfileId();
	}
}