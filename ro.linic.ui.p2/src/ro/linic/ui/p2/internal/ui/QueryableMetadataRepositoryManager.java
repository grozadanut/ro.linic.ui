package ro.linic.ui.p2.internal.ui;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An object that queries a particular set of metadata repositories.
 */
public class QueryableMetadataRepositoryManager extends QueryableRepositoryManager<IInstallableUnit> {

	public QueryableMetadataRepositoryManager(final ProvisioningUI ui, final boolean includeDisabledRepos) {
		super(ui, includeDisabledRepos);
	}

	@Override
	protected IMetadataRepository getRepository(final IRepositoryManager<IInstallableUnit> manager, final URI location) {
		// note the use of MetadataRepositoryManager (the concrete implementation).
		if (manager instanceof MetadataRepositoryManager) {
			return ((MetadataRepositoryManager) manager).getRepository(location);
		}
		return null;
	}

	@Override
	protected IMetadataRepositoryManager getRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(getSession());
	}

	@Override
	protected IMetadataRepository doLoadRepository(final IRepositoryManager<IInstallableUnit> manager, final URI location,
			final IProgressMonitor monitor) throws ProvisionException {
		return ui.loadMetadataRepository(location, false, monitor);
	}

	@Override
	protected int getRepositoryFlags(final RepositoryTracker repositoryManipulator) {
		return repositoryManipulator.getMetadataRepositoryFlags();
	}
}
