package ro.linic.ui.base.handlers;

import static ro.flexbiz.util.commons.PresentationUtils.NEWLINE;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import ro.linic.ui.base.Messages;
import ro.linic.ui.base.dialogs.InfoDialog;

public class AboutHandler {
	@Execute
	public void execute(final Shell shell) {
		final StringBuilder bundleNicenames = new StringBuilder();
		for (final Bundle bundle : FrameworkUtil.getBundle(getClass()).getBundleContext().getBundles()) {
			final Version v = bundle.getVersion();
			final String bundleNicename = String.format("%s %d.%d.%d", bundle.getSymbolicName(),
					v.getMajor(), v.getMinor(), v.getMicro());
			bundleNicenames.append(bundleNicename).append(NEWLINE);
		}
		
		InfoDialog.open(shell, Messages.About, bundleNicenames.toString());
	}
}
