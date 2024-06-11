package ro.linic.ui.base.services.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

public class TitleAreaDialogValidated extends TitleAreaDialog {
	public TitleAreaDialogValidated(final Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void setErrorMessage(final String newErrorMessage) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null)
			okButton.setEnabled(newErrorMessage == null);
		super.setErrorMessage(newErrorMessage);
	}
}
