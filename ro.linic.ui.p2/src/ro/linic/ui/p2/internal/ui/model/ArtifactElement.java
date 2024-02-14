package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * Element wrapper class for an artifact key and its repository
 *
 * @since 3.4
 */
public class ArtifactElement extends ProvElement {

	IArtifactKey key;
	IArtifactRepository repo;

	public ArtifactElement(final IEclipseContext ctx, final Object parent, final IArtifactKey key, final IArtifactRepository repo) {
		super(ctx, parent);
		this.key = key;
		this.repo = repo;
	}

	@Override
	protected String getImageId(final Object obj) {
		return null;
	}

	@Override
	public String getLabel(final Object o) {
		return key.getId() + " [" + key.getClassifier() + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public Object[] getChildren(final Object o) {
		return repo.getArtifactDescriptors(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IArtifactRepository.class)
			return (T) getArtifactRepository();
		if (adapter == IArtifactKey.class)
			return (T) getArtifactKey();
		return super.getAdapter(adapter);
	}

	public IArtifactKey getArtifactKey() {
		return key;
	}

	public IArtifactRepository getArtifactRepository() {
		return repo;
	}
}
