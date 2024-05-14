package ro.linic.ui.e4.help.browser;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.browser.IBrowserFactory;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.FrameworkUtil;

import ro.linic.ui.e4.help.internal.HelpUIEventLoop;

public class EmbeddedBrowserFactory implements IBrowserFactory {
	private boolean tested = false;

	private boolean available = false;
	private String browserType;

	/**
	 * Constructor.
	 */
	public EmbeddedBrowserFactory() {
		super();
	}

	@Override
	public boolean isAvailable() {
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_STANDALONE) {
			try {
				if (HelpUIEventLoop.isRunning()) {
					Display.getDefault().syncExec(this::test);
				}
			} catch (final Exception e) {
				// just in case
			}
		} else {
			test();
		}
		tested = true;
		return available;
	}

	/**
	 * Must run on UI thread
	 */
	private boolean test() {
		if (!Constants.OS_WIN32.equalsIgnoreCase(Platform.getOS())
				&& !Constants.OS_LINUX.equalsIgnoreCase(Platform.getOS())) {
			return false;
		}
		if (!tested) {
			tested = true;
			final Shell sh = new Shell();
			try {
				final Browser browser = new Browser(sh, SWT.NONE);
				browserType = browser.getBrowserType();
				available = true;
			} catch (final SWTError se) {
				if (se.code == SWT.ERROR_NO_HANDLES) {
					// Browser not implemented
					available = false;
				} else {
					final String bundleSymbolicName = FrameworkUtil.getBundle(getClass()).getSymbolicName();
					final Status errorStatus = new Status(IStatus.WARNING, bundleSymbolicName, IStatus.OK,
							"An error occurred during creation of embedded help browser.", new Exception(se)); //$NON-NLS-1$
					ILog.of(getClass()).log(errorStatus);
				}
			} catch (final Exception e) {
				// Browser not implemented
			}
			if (sh != null && !sh.isDisposed())
				sh.dispose();
		}
		return available;
	}

	@Override
	public IBrowser createBrowser() {
		return new EmbeddedBrowserAdapter(browserType);
	}
}
