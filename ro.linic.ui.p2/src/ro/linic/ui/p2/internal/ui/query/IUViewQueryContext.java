package ro.linic.ui.p2.internal.ui.query;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * IUViewQueryContext defines the different ways IUs can be viewed. Clients can
 * use this context to control how the various IU views are represented and
 * traversed.
 *
 * @since 2.0
 */
public class IUViewQueryContext {
	public static final int AVAILABLE_VIEW_BY_CATEGORY = 1;
	public static final int AVAILABLE_VIEW_BY_REPO = 2;
	public static final int AVAILABLE_VIEW_FLAT = 3;

	// Available view settings
	// Default available view to repo as this provides the fastest information
	private int view = AVAILABLE_VIEW_BY_REPO;
	// Whether to show latest versions only, defaults to
	// true. Clients typically use a pref setting or dialog
	// setting to initialize
	private boolean showLatestVersionsOnly = true;
	// Whether to hide things that are already installed
	// Defaults to false since we wouldn't know what profile to use
	private boolean hideAlreadyInstalled = false;
	// Whether to group items in repos by category. Note this only makes sense when
	// the
	// view type is AVAILABLE_VIEW_BY_REPO
	private boolean useCategories = true;
	// Whether to drill down into installed items
	private boolean showInstallChildren = true;
	// Whether to drill down into available items
	private boolean showAvailableChildren = false;
	// Whether to drill down into items in a provisioning plan
	private boolean showProvisioningPlanChildren = true;
	// Whether to filter out IUs based on the environment settings of the target
	// profile
	private boolean filterOnEnv = false;

	private String profileId = null;

	private String hidingInstalledDescription = ProvUIMessages.IUViewQueryContext_AllAreInstalledDescription;
	private String groupingCategoriesDescription = ProvUIMessages.IUViewQueryContext_NoCategorizedItemsDescription;

	public IUViewQueryContext(final int viewType) {
		this.view = viewType;
	}

	/*
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueryContext#
	 * getQueryType()
	 */
	public int getQueryType() {
		if (view == AVAILABLE_VIEW_BY_REPO)
			return QueryProvider.METADATA_REPOS;
		return QueryProvider.AVAILABLE_IUS;
	}

	public int getViewType() {
		return view;
	}

	public void setViewType(final int viewType) {
		view = viewType;
	}

	public boolean getShowLatestVersionsOnly() {
		return showLatestVersionsOnly;
	}

	public void setShowLatestVersionsOnly(final boolean showLatest) {
		showLatestVersionsOnly = showLatest;
	}

	public void setHideAlreadyInstalled(final boolean hide) {
		hideAlreadyInstalled = hide;
	}

	public boolean getHideAlreadyInstalled() {
		return hideAlreadyInstalled;
	}

	public String getInstalledProfileId() {
		return profileId;
	}

	public void setInstalledProfileId(final String profileId) {
		this.profileId = profileId;
	}

	public void setFilterOnEnv(final boolean filterOnEnv) {
		this.filterOnEnv = filterOnEnv;
	}

	public boolean getFilterOnEnv() {
		return filterOnEnv;
	}

	/**
	 * Set a boolean that indicates whether categories should be used when viewing
	 * by repository.
	 *
	 * useCategories <code>true</code> if a site in a sites view should expand into
	 * categories, <code>false</code> if it should expand into IU's.
	 */

	public void setUseCategories(final boolean useCategories) {
		this.useCategories = useCategories;
	}

	/**
	 * Return a boolean that indicates whether categories should be used when
	 * viewing by repository.
	 *
	 * @return <code>true</code> if a site in a sites view should expand into
	 *         categories, <code>false</code> if it should expand into IU's.
	 */
	public boolean getUseCategories() {
		return useCategories;
	}

	public boolean getShowInstallChildren() {
		return showInstallChildren;
	}

	public void setShowInstallChildren(final boolean showChildren) {
		showInstallChildren = showChildren;
	}

	public boolean getShowAvailableChildren() {
		return showAvailableChildren;
	}

	public void setShowAvailableChildren(final boolean showChildren) {
		showAvailableChildren = showChildren;
	}

	public boolean getShowProvisioningPlanChildren() {
		return showProvisioningPlanChildren;
	}

	public void setShowProvisioningPlanChildren(final boolean showChildren) {
		showProvisioningPlanChildren = showChildren;
	}

	public String getHidingInstalledDescription() {
		return hidingInstalledDescription;
	}

	public void setHidingInstalledDescription(final String description) {
		hidingInstalledDescription = description;
	}

	public String getUsingCategoriesDescription() {
		return groupingCategoriesDescription;
	}

	public void setUsingCategoriesDescription(final String description) {
		groupingCategoriesDescription = description;
	}

	public boolean shouldGroupByCategories() {
		return view == AVAILABLE_VIEW_BY_CATEGORY || (view == AVAILABLE_VIEW_BY_REPO && useCategories);
	}
}
