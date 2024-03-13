package ro.linic.ui.pos.base.preferences;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.FrameworkUtil;

import com.opcoach.e4.preferences.IPreferenceStoreProvider;
import com.opcoach.e4.preferences.ScopedPreferenceStore;

public class PreferenceStoreProvider implements IPreferenceStoreProvider {

	@Override
	public IPreferenceStore getPreferenceStore() {
		return new ScopedPreferenceStore(ConfigurationScope.INSTANCE, FrameworkUtil.getBundle(getClass()).getSymbolicName());
	}
}
