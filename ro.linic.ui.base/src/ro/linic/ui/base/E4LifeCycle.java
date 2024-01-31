package ro.linic.ui.base;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class E4LifeCycle {
	@PostContextCreate
	void postContextCreate(final BundleContext workbenchContext, @Preference final IEclipsePreferences prefs, 
			final UISynchronize sync, @OSGiBundle final Bundle bundle)
	{
		
	}
}
