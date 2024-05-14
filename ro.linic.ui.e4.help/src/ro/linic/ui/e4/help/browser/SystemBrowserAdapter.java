package ro.linic.ui.e4.help.browser;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;

import ro.linic.ui.e4.help.Messages;
import ro.linic.ui.e4.help.internal.util.ErrorUtil;

/**
 * Implmentation of IBrowser interface, using org.eclipse.swt.Program
 */
public class SystemBrowserAdapter implements IBrowser {
	String[] cmdarray;

	/**
	 * Adapter constructor.
	 */
	public SystemBrowserAdapter() {
	}

	@Override
	public void close() {
	}

	@Override
	public void displayURL(final String url) {
		//		if (Constants.WS_WIN32.equalsIgnoreCase(Platform.getOS())) {
		if (!Program.launch(url)) {
			ILog.of(getClass())
					.error(
							"Browser adapter for System Browser failed.  The system has no program registered for file " //$NON-NLS-1$
									+ url
									+ ".  Change the file association or choose a different help web browser in the preferences.", //$NON-NLS-1$
							null);
			ErrorUtil.displayErrorDialog(NLS.bind(Messages.SystemBrowser_noProgramForURL, url));
		}
		//		} else {
		//			Program b = Program.findProgram("html");
		//			if (b == null || !b.execute(url)) {
		//				ErrorUtil.displayErrorDialog(
		//					HelpUIResources.getString(
		//						"SystemBrowser.noProgramForHTML",
		//						url));
		//			}
		//		}
	}

	@Override
	public boolean isCloseSupported() {
		return false;
	}

	@Override
	public boolean isSetLocationSupported() {
		return false;
	}

	@Override
	public boolean isSetSizeSupported() {
		return false;
	}

	@Override
	public void setLocation(final int x, final int y) {
	}

	@Override
	public void setSize(final int width, final int height) {
	}
}
