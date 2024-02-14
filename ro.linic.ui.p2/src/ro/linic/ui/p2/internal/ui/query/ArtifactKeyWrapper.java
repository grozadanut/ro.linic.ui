package ro.linic.ui.p2.internal.ui.query;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

import ro.linic.ui.p2.internal.ui.model.ArtifactElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElementWrapper;

/**
 * Wrapper that accepts artifact keys and wraps them in an ArtifactKeyElement.
 *
 * @since 3.6
 */
public class ArtifactKeyWrapper extends QueriedElementWrapper {

	IArtifactRepository repo;

	public ArtifactKeyWrapper(final IEclipseContext ctx, final IArtifactRepository repo, final Object parent) {
		super(ctx, repo, parent);
		this.repo = repo;
	}

	@Override
	protected boolean shouldWrap(final Object match) {
		if ((match instanceof IArtifactKey))
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	@Override
	protected Object wrap(final Object item) {
		return super.wrap(new ArtifactElement(ctx, parent, (IArtifactKey) item, repo));
	}

}
