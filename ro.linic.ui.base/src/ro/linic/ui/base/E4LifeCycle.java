package ro.linic.ui.base;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;

import ro.linic.ui.p2.ui.Policy;

public class E4LifeCycle {

	@PostContextCreate
	void postContextCreate(final IEclipseContext ctx, @Preference final IEclipsePreferences prefs, 
			final UISynchronize sync) {
		registerP2Policy(ctx);
	}
	
	private void registerP2Policy(final IEclipseContext ctx) {
		ctx.set(Policy.class, new CloudPolicy(ctx));
	}
}
