package ro.linic.ui.p2.internal.ui;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * An object that queries a particular set of artifact repositories.
 */
public class QueryableArtifactRepositoryManager extends QueryableRepositoryManager<IArtifactKey> {

	public QueryableArtifactRepositoryManager(final ProvisioningUI ui, final boolean includeDisabledRepos) {
		super(ui, includeDisabledRepos);
	}

	@Override
	protected IArtifactRepositoryManager getRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(getSession());
	}

	@Override
	protected IArtifactRepository doLoadRepository(final IRepositoryManager<IArtifactKey> manager, final URI location,
			final IProgressMonitor monitor) throws ProvisionException {
		return ui.loadArtifactRepository(location, false, monitor);
	}

	@Override
	protected int getRepositoryFlags(final RepositoryTracker repositoryManipulator) {
		return repositoryManipulator.getArtifactRepositoryFlags();
	}

	@Override
	protected IArtifactRepository getRepository(final IRepositoryManager<IArtifactKey> manager, final URI location) {
		// note the use of ArtifactRepositoryManager (the concrete implementation).
		if (manager instanceof ArtifactRepositoryManager) {
			return ((ArtifactRepositoryManager) manager).getRepository(location);
		}
		return null;
	}
}
