package ro.linic.ui.base.preferences;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import ro.linic.ui.base.services.preferences.PreferenceKey;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final Logger log = Logger.getLogger(PreferenceInitializer.class.getName());

	@Override
	public void initializeDefaultPreferences() {
		final IEclipsePreferences node = DefaultScope.INSTANCE
				.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());

		if (node != null) {
			node.put(PreferenceKey.SERVER_BASE_URL, PreferenceKey.SERVER_BASE_URL_DEF);

			try {
				node.flush();
			} catch (final BackingStoreException e) {
				log.log(Level.SEVERE, "Error when initializing default preferences", e);
			}
		}
	}
}