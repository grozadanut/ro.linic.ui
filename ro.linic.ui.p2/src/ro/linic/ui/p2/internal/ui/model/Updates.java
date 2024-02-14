package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.QueryProvider;

/**
 * Element class that represents available updates.
 *
 * @since 3.4
 *
 */
public class Updates extends QueriedElement {

	private String profileId;
	private IInstallableUnit[] iusToBeUpdated;

	public Updates(final IEclipseContext ctx, final String profileId) {
		this(ctx, profileId, null);
	}

	public Updates(final IEclipseContext ctx, final String profileId, final IInstallableUnit[] iusToBeUpdated) {
		super(ctx, null);
		this.profileId = profileId;
		this.iusToBeUpdated = iusToBeUpdated;
	}

	@Override
	public String getLabel(final Object o) {
		return ProvUIMessages.Updates_Label;
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_UPDATES;
	}

	public String getProfileId() {
		return profileId;
	}

	public IInstallableUnit[] getIUs() {
		return iusToBeUpdated;
	}

}
