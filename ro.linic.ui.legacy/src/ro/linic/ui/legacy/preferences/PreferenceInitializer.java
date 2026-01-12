package ro.linic.ui.legacy.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        final IEclipsePreferences node = DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
    	
		if (node != null)
		{
			node.put(PreferenceKey.REGISTER_Z_DIALOG_KEY, PreferenceKey.REGISTER_Z_DIALOG_STANDARD_VALUE);
			node.put(PreferenceKey.BROTHER_PRINT_FOLDER, PreferenceKey.BROTHER_PRINT_FOLDER_DEF);
			node.putBoolean(PreferenceKey.RECEPTIE_GROUPBY_VAT_KEY, false);
			try { node.flush();  }  catch (final BackingStoreException e) { }
		}	
    }
}