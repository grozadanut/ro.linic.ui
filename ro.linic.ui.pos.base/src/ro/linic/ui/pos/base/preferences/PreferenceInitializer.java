package ro.linic.ui.pos.base.preferences;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final Logger log = Logger.getLogger(PreferenceInitializer.class.getName());

    @Override
    public void initializeDefaultPreferences() {
        final IEclipsePreferences node = DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
    	
		if (node != null)
		{
			node.put(PreferenceKey.ECR_MODEL, PreferenceKey.ECR_MODEL_DEF);
			
			node.put(PreferenceKey.DUDE_ECR_FOLDER, PreferenceKey.DUDE_ECR_FOLDER_DEF);
			node.put(PreferenceKey.DUDE_ECR_PORT, PreferenceKey.DUDE_ECR_PORT_DEF);
			node.put(PreferenceKey.DUDE_ECR_OPERATOR, PreferenceKey.DUDE_ECR_OPERATOR_DEF);
			node.put(PreferenceKey.DUDE_ECR_PASSWORD, PreferenceKey.DUDE_ECR_PASSWORD_DEF);
			node.put(PreferenceKey.DUDE_ECR_NR_AMEF, PreferenceKey.DUDE_ECR_NR_AMEF_DEF);
			node.put(PreferenceKey.DUDE_ECR_TAX_CODE, PreferenceKey.DUDE_ECR_TAX_CODE_DEF);
			node.put(PreferenceKey.DUDE_ECR_DEPT, PreferenceKey.DUDE_ECR_DEPT_DEF);
			node.putBoolean(PreferenceKey.DUDE_REPORT_Z_AND_D, PreferenceKey.DUDE_REPORT_Z_AND_D_DEF);
			
			node.put(PreferenceKey.FISCAL_NET_COMMAND_FOLDER, PreferenceKey.FISCAL_NET_COMMAND_FOLDER_DEF);
			node.put(PreferenceKey.FISCAL_NET_RESPONSE_FOLDER, PreferenceKey.FISCAL_NET_RESPONSE_FOLDER_DEF);
			node.put(PreferenceKey.FISCAL_NET_TAX_CODE, PreferenceKey.FISCAL_NET_TAX_CODE_DEF);
			node.put(PreferenceKey.FISCAL_NET_DEPT, PreferenceKey.FISCAL_NET_DEPT_DEF);
			
			try {
				node.flush();
			} catch (final BackingStoreException e) {
				log.log(Level.SEVERE, "Error when initializing default preferences", e);
			}
		}	
    }
}