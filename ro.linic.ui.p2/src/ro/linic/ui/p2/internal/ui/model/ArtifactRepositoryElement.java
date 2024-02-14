package ro.linic.ui.p2.internal.ui.model;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * Element wrapper class for a artifact repository that gets its contents in a
 * deferred manner.
 *
 * @since 3.4
 */
public class ArtifactRepositoryElement extends RemoteQueriedElement implements IRepositoryElement<IArtifactKey> {

	URI location;
	IArtifactRepository repo;
	boolean isEnabled;

	public ArtifactRepositoryElement(final IEclipseContext ctx, final Object parent, final URI location) {
		this(ctx, parent, location, true);
	}

	public ArtifactRepositoryElement(final IEclipseContext ctx, final Object parent, final URI location, final boolean isEnabled) {
		super(ctx, parent);
		this.location = location;
		this.isEnabled = isEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IArtifactRepository.class)
			return (T) getRepository(null);
		if (adapter == IRepository.class)
			return (T) getRepository(null);
		return super.getAdapter(adapter);
	}

	@Override
	protected String getImageId(final Object obj) {
		return ProvUIImages.IMG_ARTIFACT_REPOSITORY;
	}

	@Override
	public String getLabel(final Object o) {
		final String name = getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		return URIUtil.toUnencodedString(getLocation());
	}

	@Override
	public IArtifactRepository getRepository(final IProgressMonitor monitor) {
		if (repo == null)
			try {
				repo = getArtifactRepositoryManager().loadRepository(location, monitor);
			} catch (final ProvisionException e) {
				getProvisioningUI().getRepositoryTracker().reportLoadFailure(location, e);
			} catch (final OperationCanceledException e) {
				// nothing to report
			}
		return repo;
	}

	@Override
	public URI getLocation() {
		return location;
	}

	@Override
	public String getName() {
		String name = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		if (name == null)
			name = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NAME);
		if (name == null)
			name = ""; //$NON-NLS-1$
		return name;
	}

	@Override
	public String getDescription() {
		if (getProvisioningUI().getRepositoryTracker().hasNotFoundStatusBeenReported(location))
			return ProvUIMessages.RepositoryElement_NotFound;
		final String description = getArtifactRepositoryManager().getRepositoryProperty(location,
				IRepository.PROP_DESCRIPTION);
		if (description == null)
			return ""; //$NON-NLS-1$
		return description;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		isEnabled = enabled;
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_ARTIFACTS;
	}

	@Override
	public IQueryable<?> getQueryable() {
		if (queryable == null)
			queryable = getRepository(new NullProgressMonitor());
		return queryable;
	}

	IArtifactRepositoryManager getArtifactRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(getProvisioningUI().getSession());
	}
}
