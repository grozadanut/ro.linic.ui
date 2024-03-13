package ro.linic.ui.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.LogManager;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.osgi.framework.FrameworkUtil;

import ro.linic.ui.p2.ui.Policy;

public class E4LifeCycle {

	@PostContextCreate
	void postContextCreate(final IEclipseContext ctx, @Preference final IEclipsePreferences prefs, 
			final UISynchronize sync) {
		registerLogHandler();
		registerP2Policy(ctx);
	}
	
	private void registerLogHandler() {
		try {
			final URL baseUrl = FrameworkUtil.getBundle(getClass()).getEntry("logging.properties");
			final URL localURL = FileLocator.toFileURL(baseUrl);
            LogManager.getLogManager().readConfiguration(new FileInputStream(localURL.getFile()));
        } catch (IOException | SecurityException ex) {
        	ex.printStackTrace();
        }
	}

	private void registerP2Policy(final IEclipseContext ctx) {
		ctx.set(Policy.class, new CloudPolicy(ctx));
	}
}
