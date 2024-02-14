package ro.linic.ui.p2.internal.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.osgi.util.NLS;

import ro.linic.ui.p2.internal.ui.ElementWrapper;
import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;

/**
 * A wrapper that assigns a query provider and the queryable
 * who was performing the query to the wrapped elements
 * as they are accepted.
 *
 * @since 3.4
 */
public abstract class QueriedElementWrapper extends ElementWrapper {

	protected IQueryable<?> queryable;
	protected Object parent;
	protected String emptyExplanationString;
	protected int emptyExplanationSeverity;
	protected String emptyExplanationDescription;

	public QueriedElementWrapper(final IEclipseContext ctx, final IQueryable<?> queryable, final Object parent) {
		super(ctx);
		this.queryable = queryable;
		this.parent = parent;
	}

	/**
	 * Sets an item as Queryable if it is a QueriedElement
	 */
	@Override
	protected Object wrap(final Object item) {
		if (item instanceof QueriedElement) {
			final QueriedElement element = (QueriedElement) item;
			if (!element.knowsQueryable()) {
				element.setQueryable(queryable);
			}
		}
		return item;
	}

	@Override
	public Collection<?> getElements(final Collector<?> collector) {
		// Any previously stored explanations are not valid.
		emptyExplanationString = null;
		emptyExplanationSeverity = IStatus.INFO;
		emptyExplanationDescription = null;
		if (collector.isEmpty()) {
			// Before we are even filtering out items, there is nothing in the collection.
			// All we can do is look for the most common reasons and guess.  If the collection
			// is empty and the parent is an IU, then being empty is not a big deal, it means
			// we are in drilldown.
			if (parent instanceof MetadataRepositoryElement) {
				final MetadataRepositoryElement repo = (MetadataRepositoryElement) parent;
				final RepositoryTracker manipulator = repo.getProvisioningUI().getRepositoryTracker();
				if (manipulator.hasNotFoundStatusBeenReported(repo.getLocation())) {
					return emptyExplanation(IStatus.ERROR, NLS.bind(ProvUIMessages.QueriedElementWrapper_SiteNotFound, URIUtil.toUnencodedString(repo.getLocation())), ""); //$NON-NLS-1$
				}
			}
			if (parent instanceof QueriedElement) {
				final QueriedElement element = (QueriedElement) parent;
				IUViewQueryContext context = element.getQueryContext();
				if (context == null)
					context = ProvUI.getQueryContext(element.getPolicy());
				if (parent instanceof MetadataRepositoryElement || parent instanceof MetadataRepositories) {
					if (context != null && context.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY && context.getUseCategories()) {
						return emptyExplanation(IStatus.INFO, ProvUIMessages.QueriedElementWrapper_NoCategorizedItemsExplanation, context.getUsingCategoriesDescription());
					}
					return emptyExplanation(IStatus.INFO, ProvUIMessages.QueriedElementWrapper_NoItemsExplanation, null);
				}
			}
			// It is empty, but the parent is an IU, so this could be a drilldown.
			return Collections.EMPTY_LIST;
		}
		final Collection<?> elements = super.getElements(collector);
		// We had elements but now they have been filtered out.  Hopefully
		// we can explain this.
		if (elements.isEmpty()) {
			if (emptyExplanationString != null)
				return emptyExplanation(emptyExplanationSeverity, emptyExplanationString, emptyExplanationDescription);
			// We filtered out content but never explained it.  Ideally this doesn't happen if
			// all wrappers explain any filtering.
			return emptyExplanation(emptyExplanationSeverity, ProvUIMessages.QueriedElementWrapper_NoItemsExplanation, null);
		}
		return elements;
	}

	Collection<?> emptyExplanation(final int severity, final String explanationString, final String explanationDescription) {
		final ArrayList<Object> collection = new ArrayList<>(1);
		collection.add(new EmptyElementExplanation(ctx, parent, severity, explanationString, explanationDescription));
		return collection;
	}
}
