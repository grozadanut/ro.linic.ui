package ro.linic.ui.p2.internal.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

import ro.linic.ui.p2.internal.ui.model.CategoryElement;
import ro.linic.ui.p2.internal.ui.model.IIUElement;
import ro.linic.ui.p2.internal.ui.model.MetadataRepositories;
import ro.linic.ui.p2.internal.ui.model.MetadataRepositoryElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElement;
import ro.linic.ui.p2.internal.ui.model.Updates;
import ro.linic.ui.p2.internal.ui.query.ArtifactKeyWrapper;
import ro.linic.ui.p2.internal.ui.query.ArtifactRepositoryElementWrapper;
import ro.linic.ui.p2.internal.ui.query.AvailableIUWrapper;
import ro.linic.ui.p2.internal.ui.query.CategoryElementWrapper;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;
import ro.linic.ui.p2.internal.ui.query.InstalledIUElementWrapper;
import ro.linic.ui.p2.internal.ui.query.MetadataRepositoryElementWrapper;
import ro.linic.ui.p2.internal.ui.query.ProfileElementWrapper;
import ro.linic.ui.p2.internal.ui.query.QueryableProfileRegistry;
import ro.linic.ui.p2.internal.ui.query.QueryableUpdates;
import ro.linic.ui.p2.internal.ui.query.RequiredIUsQuery;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Provides a default set of queries to drive the provisioning UI.
 *
 * @since 3.5
 */

public class QueryProvider {

	private ProvisioningUI ui;
	private IEclipseContext ctx;

	public static final int METADATA_REPOS = 1;
	public static final int ARTIFACT_REPOS = 2;
	public static final int PROFILES = 3;
	public static final int AVAILABLE_IUS = 4;
	public static final int AVAILABLE_UPDATES = 5;
	public static final int INSTALLED_IUS = 6;
	public static final int AVAILABLE_ARTIFACTS = 7;

	public QueryProvider(final IEclipseContext ctx, final ProvisioningUI ui) {
		this.ctx = ctx;
		this.ui = ui;
	}

	/*
	 * Return a map of key/value pairs which are set to the environment settings
	 * for the given profile. May return <code>null</code> or an empty <code>Map</code>
	 * if the settings cannot be obtained.
	 */
	private static Map<String, String> getEnvFromProfile(final IProfile profile) {
		if (profile == null)
			return null;
		final String environments = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
		if (environments == null)
			return null;
		final Map<String, String> result = new HashMap<>();
		for (final StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) { //$NON-NLS-1$
			final String entry = tokenizer.nextToken();
			final int i = entry.indexOf('=');
			final String key = entry.substring(0, i).trim();
			final String value = entry.substring(i + 1).trim();
			result.put(key, value);
		}
		return result;
	}

	// If we are supposed to filter out the results based on the environment settings in
	// the target profile then create a compound query otherwise just return the given query
	private IQuery<IInstallableUnit> createEnvironmentFilterQuery(final IUViewQueryContext context, final IProfile profile, final IQuery<IInstallableUnit> query) {
		if (!context.getFilterOnEnv())
			return query;
		final Map<String, String> environment = getEnvFromProfile(profile);
		if (environment == null)
			return query;
		final IInstallableUnit envIU = InstallableUnit.contextIU(environment);
		final IQuery<IInstallableUnit> filterQuery = QueryUtil.createMatchQuery("filter == null || $0 ~= filter", envIU); //$NON-NLS-1$
		return QueryUtil.createCompoundQuery(query, filterQuery, true);
	}

	public ElementQueryDescriptor getQueryDescriptor(final QueriedElement element) {
		// Initialize queryable, queryContext, and queryType from the element.
		// In some cases we override this.
		final Policy policy = ui.getPolicy();
		IQueryable<?> queryable = element.getQueryable();
		final int queryType = element.getQueryType();
		IUViewQueryContext context = element.getQueryContext();
		if (context == null) {
			context = ProvUI.getQueryContext(policy);
			context.setInstalledProfileId(ui.getProfileId());
		}
		switch (queryType) {
			case ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager(ui, false).locationsQueriable();
				return new ElementQueryDescriptor(queryable, new RepositoryLocationQuery(), new Collector<>(), new ArtifactRepositoryElementWrapper(ctx, null, element));

			case AVAILABLE_IUS :
				// Things get more complicated if the user wants to filter out installed items.
				// This involves setting up a secondary query for installed content that the various
				// collectors will use to reject content.  We can't use a compound query because the
				// queryables are different (profile for installed content, repo for available content)
				AvailableIUWrapper availableIUWrapper;
				final boolean showLatest = context.getShowLatestVersionsOnly();
				final boolean hideInstalled = context.getHideAlreadyInstalled();
				IProfile targetProfile = null;
				final String profileId = context.getInstalledProfileId();
				if (profileId != null) {
					targetProfile = ProvUI.getProfileRegistry(ui.getSession()).getProfile(profileId);
				}

				IQuery<IInstallableUnit> topLevelQuery = policy.getVisibleAvailableIUQuery();
				IQuery<IInstallableUnit> categoryQuery = QueryUtil.createIUCategoryQuery();

				topLevelQuery = createEnvironmentFilterQuery(context, targetProfile, topLevelQuery);
				categoryQuery = createEnvironmentFilterQuery(context, targetProfile, categoryQuery);

				// Showing child IU's of a group of repositories, or of a single repository
				if (element instanceof MetadataRepositories || element instanceof MetadataRepositoryElement) {
					if (context.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_FLAT || !context.getUseCategories()) {
						final AvailableIUWrapper wrapper = new AvailableIUWrapper(ctx, queryable, element, false, context.getShowAvailableChildren());
						if (showLatest)
							topLevelQuery = QueryUtil.createLatestQuery(topLevelQuery);
						if (targetProfile != null)
							wrapper.markInstalledIUs(targetProfile, hideInstalled);
						return new ElementQueryDescriptor(queryable, topLevelQuery, new Collector<>(), wrapper);
					}
					// Installed content not a concern for collecting categories
					return new ElementQueryDescriptor(queryable, categoryQuery, new Collector<>(), new CategoryElementWrapper(ctx, queryable, element));
				}

				// If it's a category or some other IUElement to drill down in, we get the requirements and show all requirements
				// that are also visible in the available list.
				if (element instanceof CategoryElement || (element instanceof IIUElement && ((IIUElement) element).shouldShowChildren())) {
					// children of a category should drill down according to the context.  If we aren't in a category, we are already drilling down and
					// continue to do so.
					final boolean drillDownTheChildren = element instanceof CategoryElement ? context.getShowAvailableChildren() : true;
					IQuery<IInstallableUnit> memberOfCategoryQuery;
					if (element instanceof CategoryElement) {
						// We need an expression that uses the requirements of the element's requirements, which could be merged
						// from multiple category IUs shown as one in the UI.
						final IExpression matchesRequirementsExpression = ExpressionUtil.parse("$0.exists(r | this ~= r)"); //$NON-NLS-1$
						memberOfCategoryQuery = QueryUtil.createMatchQuery(matchesRequirementsExpression, ((CategoryElement) element).getRequirements());
					} else {
						memberOfCategoryQuery = QueryUtil.createIUCategoryMemberQuery(((IIUElement) element).getIU());
					}
					memberOfCategoryQuery = createEnvironmentFilterQuery(context, targetProfile, memberOfCategoryQuery);
					availableIUWrapper = new AvailableIUWrapper(ctx, queryable, element, true, drillDownTheChildren);
					if (targetProfile != null)
						availableIUWrapper.markInstalledIUs(targetProfile, hideInstalled);
					// if it's a category, there is a special query.
					if (element instanceof CategoryElement) {
						if (showLatest)
							memberOfCategoryQuery = QueryUtil.createLatestQuery(memberOfCategoryQuery);
						return new ElementQueryDescriptor(queryable, memberOfCategoryQuery, new Collector<>(), availableIUWrapper);
					}
					// It is not a category, we want to traverse the requirements that are groups.
					IQuery<IInstallableUnit> query = QueryUtil.createCompoundQuery(topLevelQuery, new RequiredIUsQuery(((IIUElement) element).getIU()), true);
					if (showLatest)
						query = QueryUtil.createLatestQuery(query);
					// If it's not a category, these are generic requirements and should be filtered by the visibility property (topLevelQuery)
					return new ElementQueryDescriptor(queryable, query, new Collector<>(), availableIUWrapper);
				}
				return null;

			case AVAILABLE_UPDATES :
				// This query can be used by the automatic updater in headless cases (checking for updates).
				// We traffic in IU's rather than wrapped elements
				IProfile profile;
				IInstallableUnit[] toUpdate = null;
				if (element instanceof Updates) {
					profile = ProvUI.getProfileRegistry(ui.getSession()).getProfile(((Updates) element).getProfileId());
					toUpdate = ((Updates) element).getIUs();
				} else {
					profile = ProvUI.getAdapter(element, IProfile.class);
				}
				if (profile == null)
					return null;
				if (toUpdate == null) {
					final IQueryResult<IInstallableUnit> queryResult = profile.query(policy.getVisibleInstalledIUQuery(), null);
					toUpdate = queryResult.toArray(IInstallableUnit.class);
				}
				final QueryableUpdates updateQueryable = new QueryableUpdates(ui, toUpdate);
				return new ElementQueryDescriptor(updateQueryable, context.getShowLatestVersionsOnly() ? QueryUtil.createLatestIUQuery() : QueryUtil.createIUAnyQuery(), new Collector<>());

			case INSTALLED_IUS :
				// Querying of IU's.  We are drilling down into the requirements.
				if (element instanceof IIUElement && context.getShowInstallChildren()) {
					final Collection<IRequirement> reqs = ((IIUElement) element).getRequirements();
					if (reqs.size() == 0)
						return null; // no children
					final IExpression[] requirementExpressions = new IExpression[reqs.size()];
					int i = 0;
					for (final IRequirement req : reqs) {
						requirementExpressions[i++] = req.getMatches();
					}

					final IExpressionFactory factory = ExpressionUtil.getFactory();
					final IQuery<IInstallableUnit> meetsAnyRequirementQuery = QueryUtil.createMatchQuery(factory.or(requirementExpressions));
					final IQuery<IInstallableUnit> visibleAsAvailableQuery = policy.getVisibleAvailableIUQuery();
					final IQuery<IInstallableUnit> createCompoundQuery = QueryUtil.createCompoundQuery(visibleAsAvailableQuery, meetsAnyRequirementQuery, true);
					return new ElementQueryDescriptor(queryable, createCompoundQuery, new Collector<>(), new InstalledIUElementWrapper(ctx, queryable, element));
				}
				profile = ProvUI.getAdapter(element, IProfile.class);
				if (profile == null)
					return null;
				return new ElementQueryDescriptor(profile, policy.getVisibleInstalledIUQuery(), new Collector<>(), new InstalledIUElementWrapper(ctx, profile, element));

			case METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					if (queryable == null) {
						queryable = new QueryableMetadataRepositoryManager(ui, ((MetadataRepositories) element).getIncludeDisabledRepositories()).locationsQueriable();
						element.setQueryable(queryable);
					}
					return new ElementQueryDescriptor(element.getQueryable(), new RepositoryLocationQuery(), new Collector<>(), new MetadataRepositoryElementWrapper(ctx, null, element));
				}
				return null;

			case PROFILES :
				queryable = new QueryableProfileRegistry(ui);
				return new ElementQueryDescriptor(queryable, QueryUtil.createMatchQuery(IProfile.class, ExpressionUtil.TRUE_EXPRESSION), new Collector<>(), new ProfileElementWrapper(ctx, null, element));

			case AVAILABLE_ARTIFACTS :
				if (!(queryable instanceof IArtifactRepository))
					return null;
				return new ElementQueryDescriptor(queryable, ArtifactKeyQuery.ALL_KEYS, new Collector<>(), new ArtifactKeyWrapper(ctx, (IArtifactRepository) queryable, element));

			default :
				return null;
		}
	}
}
