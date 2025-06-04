package ro.linic.ui.base.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.base.Messages;
import ro.linic.ui.base.dialogs.InfoDialog;
import ro.linic.ui.base.services.util.UIUtils;

public class AboutHandler {
	@Execute
	public void execute(final Shell shell) {
		InfoDialog.open(shell, Messages.About, UIUtils.bundleNicenames());
	}
}
