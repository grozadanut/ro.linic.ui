package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.e4.core.contexts.IEclipseContext;

import ro.linic.ui.p2.internal.ui.QueryableMetadataRepositoryManager;
import ro.linic.ui.p2.org.eclipse.ui.IDeferredWorkbenchAdapter;
import ro.linic.ui.p2.org.eclipse.ui.IElementCollector;

/**
 * Element wrapper class for objects that gets their children using a deferred
 * query.
 *
 * @since 3.4
 */
public abstract class RemoteQueriedElement extends QueriedElement implements IDeferredWorkbenchAdapter {

	protected RemoteQueriedElement(final IEclipseContext ctx, final Object parent) {
		super(ctx, parent);
	}

	@Override
	public void fetchDeferredChildren(final Object o, final IElementCollector collector, final IProgressMonitor monitor) {
		try {
			final Object[] children = fetchChildren(o, monitor);
			for (final Object child : children) {
				if (child instanceof CategoryElement) {
					((CategoryElement) child).fetchChildren(child, monitor);
				}
			}
			if (!monitor.isCanceled()) {
				collector.add(children, monitor);
			}
		} catch (final OperationCanceledException e) {
			// Nothing to do
		}
		collector.done();
	}

	@Override
	public ISchedulingRule getRule(final Object object) {
		return null;
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	/*
	 * Overridden to ensure that we check whether we are using a
	 * QueryableMetadataRepositoryManager as our queryable. If so, we must find out
	 * if it is up to date with the real manager.
	 *
	 * This is necessary to prevent background loading of already loaded
	 * repositories by the DeferredTreeContentManager, which will add redundant
	 * children to the viewer. see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=229069 see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=226343 see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=275235
	 */

	@Override
	public boolean hasQueryable() {
		if (queryable instanceof QueryableMetadataRepositoryManager)
			return ((QueryableMetadataRepositoryManager) queryable).areRepositoriesLoaded();
		return super.hasQueryable();
	}

}
