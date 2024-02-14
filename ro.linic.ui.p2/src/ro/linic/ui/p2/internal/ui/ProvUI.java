package ro.linic.ui.p2.internal.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ISizingPhaseSet;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import ro.linic.ui.p2.internal.ui.dialogs.ILayoutConstants;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.internal.ui.viewers.IUColumnConfig;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Generic provisioning UI utility and policy methods.
 *
 * @since 3.4
 */
public class ProvUI {

	// Public constants for common command and tooltip names
	public static final String INSTALL_COMMAND_LABEL = ProvUIMessages.InstallIUCommandLabel;
	public static final String INSTALL_COMMAND_TOOLTIP = ProvUIMessages.InstallIUCommandTooltip;
	public static final String UNINSTALL_COMMAND_LABEL = ProvUIMessages.UninstallIUCommandLabel;
	public static final String UNINSTALL_COMMAND_TOOLTIP = ProvUIMessages.UninstallIUCommandTooltip;
	public static final String UPDATE_COMMAND_LABEL = ProvUIMessages.UpdateIUCommandLabel;
	public static final String UPDATE_COMMAND_TOOLTIP = ProvUIMessages.UpdateIUCommandTooltip;
	public static final String REVERT_COMMAND_LABEL = ProvUIMessages.RevertIUCommandLabel;
	public static final String REVERT_COMMAND_TOOLTIP = ProvUIMessages.RevertIUCommandTooltip;

	/**
	 * A constant indicating that there was nothing to size (there was no valid plan
	 * that could be used to compute size).
	 */
	public static final long SIZE_NOTAPPLICABLE = -3L;
	/**
	 * Indicates that the size is unavailable (an attempt was made to compute size
	 * but it failed)
	 */
	public static final long SIZE_UNAVAILABLE = -2L;
	/**
	 * Indicates that the size is currently unknown
	 */
	public static final long SIZE_UNKNOWN = -1L;

	private static IUColumnConfig[] columnConfig;

	public static IStatus handleException(final Throwable t, String message, final int style) {
		if (message == null && t != null) {
			message = t.getMessage();
		}
		final IStatus status = new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, 0, message, t);
		MessageDialog.openError(ProvUI.getDefaultParentShell(), ProvUIMessages.ProvUI_WarningTitle,
				status.getMessage());
		return status;
	}

	public static void reportStatus(final IEclipseContext ctx, final IStatus status, int style) {
		// workaround for
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=211933
		// Note we'd rather have a proper looking dialog than get the
		// blocking right.
		if ((style & StatusManager.BLOCK) == StatusManager.BLOCK
				|| (style & StatusManager.SHOW) == StatusManager.SHOW) {
			if (status.getSeverity() == IStatus.INFO) {
				final MessageDialogWithLink dialog = new MessageDialogWithLink(ProvUI.getDefaultParentShell(),
						ProvUIMessages.ProvUI_InformationTitle, null, status.getMessage(), MessageDialog.INFORMATION, 0,
						IDialogConstants.OK_LABEL);
				if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
					dialog.addSelectionListener(SelectionListener.widgetSelectedAdapter(
							e -> ProvisioningUI.getDefaultUI(ctx).manipulateRepositories(dialog.getShell())));
				}
				dialog.open();
				// unset the dialog bits
				style = style & ~StatusManager.BLOCK;
				style = style & ~StatusManager.SHOW;
				// unset logging for statuses that should never be logged.
				// Ideally the caller would do this but this bug keeps coming back.
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274074
				if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
					style = 0;
			} else if (status.getSeverity() == IStatus.WARNING) {
				MessageDialog.openWarning(ProvUI.getDefaultParentShell(), ProvUIMessages.ProvUI_WarningTitle,
						status.getMessage());
				// unset the dialog bits
				style = style & ~StatusManager.BLOCK;
				style = style & ~StatusManager.SHOW;
			}
		}
		if (style != 0)
		{
			ctx.get(ILog.class).log(status);
//			MessageDialog.openWarning(ProvUI.getDefaultParentShell(), ProvUIMessages.ProvUI_WarningTitle,
//					status.getMessage());
		}
	}

	public static IUColumnConfig[] getIUColumnConfig() {
		if (columnConfig == null)
			columnConfig = new IUColumnConfig[] {
					new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME,
							ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH),
					new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION,
							ILayoutConstants.DEFAULT_COLUMN_WIDTH) };
		return columnConfig;

	}

	// Factory method returning a new instance of a IUViewQueryContext
	public static IUViewQueryContext getQueryContext(final Policy policy) {
		final IUViewQueryContext queryContext = new IUViewQueryContext(
				policy.getGroupByCategory() ? IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY
						: IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		queryContext.setShowInstallChildren(policy.getShowDrilldownRequirements());
		queryContext.setShowProvisioningPlanChildren(policy.getShowDrilldownRequirements());

		// among other things the 4 calls below are used to control the available
		// software dialog (AvailableIUPage)
		queryContext.setShowLatestVersionsOnly(policy.getShowLatestVersionsOnly());
		queryContext.setHideAlreadyInstalled(policy.getHideAlreadyInstalled());
		queryContext.setUseCategories(policy.getGroupByCategory());
		queryContext.setFilterOnEnv(policy.getFilterOnEnv());
		return queryContext;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAdapter(final Object object, final Class<T> adapterType) {
		if (object == null)
			return null;
		if (adapterType.isInstance(object))
			// Ideally, we would use Class.cast here but it was introduced in Java 1.5
			return (T) object;
		if (object instanceof IAdaptable)
			// Ideally, we would use Class.cast here but it was introduced in Java 1.5
			return ((IAdaptable) object).getAdapter(adapterType);
		return null;
	}

	/**
	 * Returns a shell that is appropriate to use as the parent for a modal dialog.
	 */
	public static Shell getDefaultParentShell() {
		return Display.getCurrent().getActiveShell();
	}

	public static boolean isUpdateManagerInstallerPresent() {
		return false;
	}

	public static boolean isCategory(final IInstallableUnit iu) {
		return QueryUtil.isCategory(iu);
	}

	/**
	 * Get sizing information about the specified plan.
	 *
	 * @param engine  the engine
	 * @param plan    the provisioning plan
	 * @param context the provisioning context to be used for the sizing
	 * @param monitor the progress monitor
	 *
	 * @return a long integer describing the disk size required for the provisioning
	 *         plan.
	 *
	 * @see #SIZE_UNKNOWN
	 * @see #SIZE_UNAVAILABLE
	 * @see #SIZE_NOTAPPLICABLE
	 */
	public static long getSize(final IEngine engine, final IProvisioningPlan plan, final ProvisioningContext context,
			final IProgressMonitor monitor) {
		// If there is nothing to size, return 0
		if (plan == null)
			return SIZE_NOTAPPLICABLE;
		if (countPlanElements(plan) == 0)
			return 0;
		long installPlanSize = 0;
		final SubMonitor mon = SubMonitor.convert(monitor, 300);
		if (plan.getInstallerPlan() != null) {
			final ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
			final IStatus status = engine.perform(plan.getInstallerPlan(), sizingPhaseSet, mon.newChild(100));
			if (status.isOK())
				installPlanSize = sizingPhaseSet.getDiskSize();
		} else {
			mon.worked(100);
		}
		final ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
		final IStatus status = engine.perform(plan, sizingPhaseSet, mon.newChild(200));
		if (status.isOK())
			return installPlanSize + sizingPhaseSet.getDiskSize();
		return SIZE_UNAVAILABLE;
	}

	private static int countPlanElements(final IProvisioningPlan plan) {
		return QueryUtil.compoundQueryable(plan.getAdditions(), plan.getRemovals())
				.query(QueryUtil.createIUAnyQuery(), null).toUnmodifiableSet().size();
	}

	/**
	 * Return the artifact repository manager for the given session
	 *
	 * @return the repository manager
	 */
	public static IArtifactRepositoryManager getArtifactRepositoryManager(final ProvisioningSession session) {
		return session.getProvisioningAgent().getService(IArtifactRepositoryManager.class);
	}

	/**
	 * Return the metadata repository manager for the given session
	 *
	 * @return the repository manager
	 */
	public static IMetadataRepositoryManager getMetadataRepositoryManager(final ProvisioningSession session) {
		return session.getProvisioningAgent().getService(IMetadataRepositoryManager.class);
	}

	/**
	 * Return the profile registry for the given session
	 *
	 * @return the profile registry
	 */
	public static IProfileRegistry getProfileRegistry(final ProvisioningSession session) {
		return session.getProvisioningAgent().getService(IProfileRegistry.class);
	}

	/**
	 * Return the provisioning engine for the given session
	 *
	 * @return the provisioning engine
	 */
	public static IEngine getEngine(final ProvisioningSession session) {
		return session.getProvisioningAgent().getService(IEngine.class);
	}

	/**
	 * Return the provisioning event bus used for dispatching events.
	 *
	 * @return the event bus
	 */
	public static IProvisioningEventBus getProvisioningEventBus(final ProvisioningSession session) {
		return session.getProvisioningAgent().getService(IProvisioningEventBus.class);
	}

	public static IProvisioningPlan toCompabilityWithCurrentJREProvisioningPlan(final ILog log,
			final ProfileChangeOperation referenceOperation, final IProgressMonitor monitor) {
		final IInstallableUnit currentJREUnit = createCurrentJavaSEUnit(log);
		final IProfileChangeRequest compatibilityWithCurrentRequest = toCurrentJREOperation(referenceOperation,
				currentJREUnit);
		final IPlanner planner = referenceOperation.getProvisioningPlan().getProfile().getProvisioningAgent()
				.getService(IPlanner.class);
		final IProvisioningPlan res = planner.getProvisioningPlan(compatibilityWithCurrentRequest,
				referenceOperation.getProvisioningContext(), monitor);
		return res;
	}

	private static IProfileChangeRequest toCurrentJREOperation(final ProfileChangeOperation operation,
			final IInstallableUnit currnetJREUnit) {
		final IProfileChangeRequest initialRequest = operation.getProfileChangeRequest();
		if (initialRequest == null) {
			throw new IllegalStateException("operation plan must be resolved"); //$NON-NLS-1$
		}
		final IProfileChangeRequest res = ((ProfileChangeRequest) initialRequest).clone();
		res.addExtraRequirements(Collections.singleton(MetadataFactory
				.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "a.jre.javase", null, null, 0, 0, false))); //$NON-NLS-1$
		operation.getProvisioningPlan().getProfile().query(QueryUtil.createIUQuery("a.jre.javase"), null) //$NON-NLS-1$
				.forEach(res::remove);
		res.add(currnetJREUnit);
		return res;
	}

	private static IInstallableUnit createCurrentJavaSEUnit(final ILog log) {
		final InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("currently-running-execution-environement-do-not-actually-install"); //$NON-NLS-1$
		final Version eeVersion = getCurrentJavaSEVersion();
		desc.setVersion(eeVersion);
		desc.addProvidedCapabilities(Collections
				.singletonList(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, desc.getId(), eeVersion)));
		desc.addProvidedCapabilities(parseSystemCapabilities(log, Constants.FRAMEWORK_SYSTEMCAPABILITIES));
		desc.addProvidedCapabilities(parseSystemCapabilities(log, Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA));
		desc.addProvidedCapabilities(toJavaPackageCapabilities(log, Constants.FRAMEWORK_SYSTEMPACKAGES));
		desc.addProvidedCapabilities(toJavaPackageCapabilities(log, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));
		return MetadataFactory.createInstallableUnit(desc);
	}

	private static List<IProvidedCapability> toJavaPackageCapabilities(final ILog log, final String systemPropertyName) {
		final String packages = System.getProperty(systemPropertyName);
		if (packages != null && !packages.trim().isEmpty()) {
			try {
				return Arrays.stream(ManifestElement.parseHeader(systemPropertyName, packages)) //
						.map(jrePackage -> {
							final String packageName = jrePackage.getValue();
							final Version packageVersion = Version.create(jrePackage.getAttribute("version")); //$NON-NLS-1$
							return MetadataFactory.createProvidedCapability("java.package", packageName, //$NON-NLS-1$
									packageVersion);
						}).collect(Collectors.toList());
			} catch (final BundleException e) {
				log.log(new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, e.getMessage(), e));
			}
		}
		return Collections.emptyList();
	}

	private static Version getCurrentJavaSEVersion() {
		final String[] segments = System.getProperty("java.version").split("\\."); //$NON-NLS-1$ //$NON-NLS-2$
		if ("1".equals(segments[0])) { //$NON-NLS-1$
			return Version.create(segments[0] + '.' + segments[1] + ".0"); //$NON-NLS-1$
		}
		return Version.create(segments[0].split("-")[0] + ".0.0"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	static Collection<IProvidedCapability> parseSystemCapabilities(final ILog log, final String systemProperty) {
		final String systemCapabilities = System.getProperty(systemProperty);
		if (systemCapabilities == null || systemCapabilities.trim().isEmpty()) {
			return Collections.emptyList();
		}
		try {
			return Arrays.stream(ManifestElement.parseHeader(systemProperty, systemCapabilities)) //
					.flatMap(eeCapability -> {
						final String eeName = eeCapability.getAttribute("osgi.ee"); //$NON-NLS-1$
						if (eeName == null) {
							return Stream.empty();
						}
						return parseEECapabilityVersion(eeCapability) //
								.map(version -> MetadataFactory.createProvidedCapability("osgi.ee", eeName, version)); //$NON-NLS-1$
					}).collect(Collectors.toList());
		} catch (final BundleException e) {
			log.log(new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, e.getMessage(), e));
			return Collections.emptyList();
		}
	}

	private static Stream<Version> parseEECapabilityVersion(final ManifestElement eeCapability) {
		final String singleVersion = eeCapability.getAttribute("version:Version"); //$NON-NLS-1$
		final String[] multipleVersions = ManifestElement
				.getArrayFromList(eeCapability.getAttribute("version:List<Version>")); //$NON-NLS-1$

		if (singleVersion == null && multipleVersions == null) {
			return Stream.empty();
		} else if (singleVersion == null) {
			return Arrays.stream(multipleVersions).map(Version::parseVersion);
		} else if (multipleVersions == null) {
			return Stream.of(singleVersion).map(Version::parseVersion);
		}
		return Stream.empty();
	}

}
