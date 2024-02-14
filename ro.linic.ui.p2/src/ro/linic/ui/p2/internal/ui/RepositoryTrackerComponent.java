package ro.linic.ui.p2.internal.ui;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.operations.RepositoryTracker;

import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Component that provides a factory that can create and initialize
 * {@link RepositoryTracker} instances.
 */
public class RepositoryTrackerComponent implements IAgentServiceFactory {

	@Override
	public Object createService(final IProvisioningAgent agent) {
		final ProvisioningUI ui = agent.getService(ProvisioningUI.class);
		if (ui == null)
			return null;
		return new ColocatedRepositoryTracker(ui);
	}
}
