package ro.linic.ui.base.preferences;

import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final Logger log = Logger.getLogger(PreferenceInitializer.class.getName());

	@Override
	public void initializeDefaultPreferences() {
		final IEclipsePreferences node = DefaultScope.INSTANCE
				.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());

		if (node != null) {

//			try {
//				node.flush();
//			} catch (final BackingStoreException e) {
//				log.log(Level.SEVERE, "Error when initializing default preferences", e);
//			}
		}
	}
}