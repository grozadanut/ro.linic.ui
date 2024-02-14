package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.e4.core.contexts.IEclipseContext;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.QueryProvider;
import ro.linic.ui.p2.internal.ui.QueryableMetadataRepositoryManager;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Element class that represents some collection of metadata repositories. It
 * can be configured so that it retrieves its children in different ways. The
 * default query type will return the metadata repositories specified in this
 * element. Other query types can be used to query each repository and aggregate
 * the children.
 *
 * @since 3.4
 *
 */
public class MetadataRepositories extends RootElement {

	private boolean includeDisabled = false;

	public MetadataRepositories(final IEclipseContext ctx, final ProvisioningUI ui) {
		this(ctx, ProvUI.getQueryContext(ui.getPolicy()), ui, null);
	}

	public MetadataRepositories(final IEclipseContext ctx, final IUViewQueryContext queryContext, final ProvisioningUI ui,
			final QueryableMetadataRepositoryManager queryable) {
		super(ctx, queryContext, ui);
		this.queryable = queryable;
	}

	/**
	 * Get whether disabled repositories should be included in queries when no
	 * repositories have been specified. This boolean is used because the flags
	 * specified when getting repositories from a repository manager are treated as
	 * an AND, and we want to permit aggregating disabled repositories along with
	 * other flags.
	 *
	 * @return includeDisabled <code>true</code> if disabled repositories should be
	 *         included and <code>false</code> if they should not be included.
	 */
	public boolean getIncludeDisabledRepositories() {
		return includeDisabled;
	}

	/**
	 * Set whether disabled repositories should be included in queries when no
	 * repositories have been specified. This boolean is used because the flags
	 * specified when getting repositories from a repository manager are treated as
	 * an AND, and we want to permit aggregating disabled repositories along with
	 * other flags.
	 *
	 * @param includeDisabled <code>true</code> if disabled repositories should be
	 *                        included and <code>false</code> if they should not be
	 *                        included.
	 */
	public void setIncludeDisabledRepositories(final boolean includeDisabled) {
		this.includeDisabled = includeDisabled;
	}

	/*
	 * Overridden to check the query context. We might be showing repositories, or
	 * we might be flattening the view to some other element
	 */
	@Override
	public int getQueryType() {
		if (getQueryContext() == null)
			return getDefaultQueryType();
		return getQueryContext().getQueryType();
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.METADATA_REPOS;
	}

	@Override
	public String getLabel(final Object o) {
		return ProvUIMessages.Label_Repositories;
	}

	/*
	 * Overridden because we might be iterating sites (type = METADATA_REPOSITORIES)
	 * rather than loading repos. If this is the case, we only care whether we have
	 * a queryable or not.
	 */
	@Override
	public boolean hasQueryable() {
		if (getQueryType() == QueryProvider.METADATA_REPOS)
			return queryable != null;
		return super.hasQueryable();
	}
}
