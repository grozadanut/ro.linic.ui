package ro.linic.ui.base;

import org.eclipse.e4.core.contexts.IEclipseContext;

import ro.linic.ui.p2.ui.Policy;

public class CloudPolicy extends Policy {
	public CloudPolicy(final IEclipseContext ctx) {
		super(ctx);
		// XXX User has no access to manipulate repositories
		setRepositoriesVisible(false);
	}
}