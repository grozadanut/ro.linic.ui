package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.jface.wizard.IWizardPage;

import ro.linic.ui.p2.internal.ui.model.ElementUtils;
import ro.linic.ui.p2.ui.AcceptLicensesWizardPage;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Common superclass for wizards that need to show licenses.
 *
 * @since 3.5
 */
public abstract class WizardWithLicenses extends ProvisioningOperationWizard {

	private static final String BYPASS_LICENSE_PAGE = "bypassLicensePage"; //$NON-NLS-1$

	AcceptLicensesWizardPage licensePage;
	boolean bypassLicensePage;

	public boolean isBypassLicensePage() {
		return bypassLicensePage;
	}

	public void setBypassLicensePage(final boolean bypassLicensePage) {
		this.bypassLicensePage = bypassLicensePage;
	}

	@Override
	public void addPages() {
		super.addPages();

		if (!bypassLicensePage) {
			licensePage = createLicensesPage();
			addPage(licensePage);
		}
	}

	public WizardWithLicenses(final IEclipseContext ctx, final ProvisioningUI ui, final ProfileChangeOperation operation, final Object[] initialSelections,
			final LoadMetadataRepositoryJob job) {
		super(ctx, ui, operation, initialSelections, job);
		this.bypassLicensePage = canBypassLicensePage();
	}

	protected AcceptLicensesWizardPage createLicensesPage() {
		IInstallableUnit[] ius = new IInstallableUnit[0];
		if (planSelections != null)
			ius = ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]);
		return new AcceptLicensesWizardPage(ctx, ui.getLicenseManager(), ius, operation);
	}

	/*
	 * Overridden to determine whether the license page should be shown.
	 */
	@Override
	public IWizardPage getNextPage(final IWizardPage page) {
		// If the license page is supposed to be the next page,
		// ensure there are actually licenses that need acceptance.
		IWizardPage proposedPage = super.getNextPage(page);

		if (!bypassLicensePage) {
			if (proposedPage == licensePage && licensePage != null) {
				if (!licensePage.hasLicensesToAccept()) {
					proposedPage = null;
				} else {
					proposedPage = licensePage;
				}
			}
		}

		return proposedPage;
	}

	@Override
	protected void planChanged() {
		super.planChanged();
		if (!bypassLicensePage) {
			licensePage.update(ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]), operation);
		}
	}

	@Override
	public boolean performFinish() {

		if (!bypassLicensePage) {
			licensePage.performFinish();
		}

		return super.performFinish();
	}

	public static boolean canBypassLicensePage() {
		final IScopeContext[] contexts = new IScopeContext[] { InstanceScope.INSTANCE, DefaultScope.INSTANCE,
				BundleDefaultsScope.INSTANCE, ConfigurationScope.INSTANCE };
		final boolean bypass = Platform.getPreferencesService().getBoolean(ProvUIAddon.PLUGIN_ID, BYPASS_LICENSE_PAGE,
				false, contexts);
		return bypass;
	}

}
