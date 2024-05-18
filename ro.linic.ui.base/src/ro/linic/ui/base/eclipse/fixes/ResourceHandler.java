package ro.linic.ui.base.eclipse.fixes;

import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.internal.runtime.PlatformURLPluginConnection;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.internal.workbench.CommandLineOptionModelProcessor;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.internal.workbench.E4XMIResource;
import org.eclipse.e4.ui.internal.workbench.E4XMIResourceFactory;
import org.eclipse.e4.ui.internal.workbench.URIHelper;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.commands.impl.CommandsPackageImpl;
import org.eclipse.e4.ui.model.application.impl.ApplicationPackageImpl;
import org.eclipse.e4.ui.model.application.ui.advanced.impl.AdvancedPackageImpl;
import org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuPackageImpl;
import org.eclipse.e4.ui.workbench.IModelResourceHandler;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This class is responsible to load and save the model
 */
public class ResourceHandler implements IModelResourceHandler {
	private static final ILog log = ILog.of(ResourceHandler.class);
	private static final String INSTALLATION_STATE_KEY = "installationState"; //$NON-NLS-1$
	
	// plugins that don't contribute to workbench.xmi, thus the app doesn't need clearing after an update
	private static final Set<String> NON_UI_PLUGINS = Set.of("ro.linic.ui.jface.localization", //$NON-NLS-1$
			"ro.linic.ui.pos.base", "ro.linic.ui.http", "ro.linic.ui.security", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"ro.linic.ui.workbench", "ro.linic.ui.p2", "ro.linic.ui.e4.help", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"ro.linic.ui.base.services"); //$NON-NLS-1$

	private ResourceSet resourceSet;
	private Resource resource;

	@Inject
	private Logger logger;

	@Inject
	private IEclipseContext context;

	@Inject
	@Named(E4Workbench.INITIAL_WORKBENCH_MODEL_URI)
	private URI applicationDefinitionInstance;

	@Inject
	@Optional
	@Named(E4Workbench.INSTANCE_LOCATION)
	private Location instanceLocation;

	@Inject
	@Optional
	@Named(IWorkbench.PERSIST_STATE)
	private boolean saveAndRestore;

	private boolean clearPersistedState;

	/**
	 * Constructor.
	 *
	 * @param saveAndRestore
	 * @param clearPersistedState
	 */
	@Inject
	public ResourceHandler(@Named(IWorkbench.PERSIST_STATE) final boolean saveAndRestore,
			@Named(IWorkbench.CLEAR_PERSISTED_STATE) final boolean clearPersistedState) {
		final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final boolean clearPersistedStatePref = node.getBoolean(IWorkbench.CLEAR_PERSISTED_STATE, false);
		
		this.saveAndRestore = saveAndRestore;
		this.clearPersistedState = clearPersistedState || clearPersistedStatePref;
	}

	@PostConstruct
	void init() {
		resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new E4XMIResourceFactory());
		resourceSet.getPackageRegistry().put(ApplicationPackageImpl.eNS_URI, ApplicationPackageImpl.eINSTANCE);
		resourceSet.getPackageRegistry().put(CommandsPackageImpl.eNS_URI, CommandsPackageImpl.eINSTANCE);
		resourceSet.getPackageRegistry().put(UiPackageImpl.eNS_URI, UiPackageImpl.eINSTANCE);
		resourceSet.getPackageRegistry().put(MenuPackageImpl.eNS_URI, MenuPackageImpl.eINSTANCE);
		resourceSet.getPackageRegistry().put(BasicPackageImpl.eNS_URI, BasicPackageImpl.eINSTANCE);
		resourceSet.getPackageRegistry().put(AdvancedPackageImpl.eNS_URI, AdvancedPackageImpl.eINSTANCE);
		resourceSet.getPackageRegistry().put(
				org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicPackageImpl.eNS_URI,
				org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicPackageImpl.eINSTANCE);

	}

	/**
	 * @return {@code true} if the current application model has top-level windows.
	 */
	public boolean hasTopLevelWindows() {
		return hasTopLevelWindows(resource);
	}

	/**
	 * @return {@code true} if the specified application model has top-level windows.
	 */
	private boolean hasTopLevelWindows(final Resource applicationResource) {
		if (applicationResource == null || applicationResource.getContents() == null) {
			// If the application resource doesn't exist or has no contents, then it has no
			// top-level windows (and we are in an error state).
			return false;
		}
		final MApplication application = (MApplication) applicationResource.getContents().get(0);
		return !application.getChildren().isEmpty();
	}

	@Override
	public Resource loadMostRecentModel() {
		File workbenchData = null;
		URI restoreLocation = null;

		if (saveAndRestore) {
			workbenchData = getWorkbenchSaveLocation();
			restoreLocation = URI.createFileURI(workbenchData.getAbsolutePath());
		}

		if ((clearPersistedState || installationChanged()) && workbenchData != null && workbenchData.exists()) {
			try {
				final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
				node.putBoolean(IWorkbench.CLEAR_PERSISTED_STATE, false);
				node.flush();
			} catch (final Exception e) {
				log.error(e.getMessage(), e);
			}
			
			workbenchData.delete();
		}

		// last stored time-stamp
		final long restoreLastModified = restoreLocation == null ? 0L : new File(
				restoreLocation.toFileString()).lastModified();

		// See bug 380663, bug 381219
		// long lastApplicationModification = getLastApplicationModification();
		// boolean restore = restoreLastModified > lastApplicationModification;
		final boolean restore = restoreLastModified > 0;
		boolean initialModel;

		resource = null;
		if (restore && saveAndRestore) {
			resource = loadResource(restoreLocation);
			// If the saved model does not have any top-level windows, Eclipse will exit
			// immediately, so throw out the persisted state and reinitialize with the defaults.
			if (!hasTopLevelWindows(resource)) {
				if (logger != null) {
					logger.error(new Exception(), // log a stack trace to help debug the corruption
							"The persisted application model has no top-level window. Reinitializing with the default application model."); //$NON-NLS-1$
				}
				resource = null;
			}
		}
		if (resource == null) {
			final Resource applicationResource = loadResource(applicationDefinitionInstance);
			final MApplication theApp = (MApplication) applicationResource.getContents().get(0);
			resource = createResourceWithApp(theApp);
			context.set(E4Workbench.NO_SAVED_MODEL_FOUND, Boolean.TRUE);
			initialModel = true;
		} else {
			initialModel = false;
		}

		// Add model items described in the model extension point
		// This has to be done before commands are put into the context
		final MApplication appElement = (MApplication) resource.getContents().get(0);

		this.context.set(MApplication.class, appElement);

		final ModelAssembler mac = context.get(ModelAssembler.class);
		if (mac != null) {
			ContextInjectionFactory.invoke(mac, PostConstruct.class, context);
			mac.processModel(initialModel);
		}

		if (!hasTopLevelWindows(resource) && logger != null) {
			logger.error(new Exception(), // log a stack trace to help debug the
											// corruption
					"Loading the application model results in no top-level window." //$NON-NLS-1$
							+ "Continuing execution, but the missing window may cause other initialization failures."); //$NON-NLS-1$
		}

		final CommandLineOptionModelProcessor processor = ContextInjectionFactory.make(CommandLineOptionModelProcessor.class, context);
		processor.process();

		return resource;
	}

	/**
	 * If the version of any UI plugin changes, or the list of UI plugins changes, 
	 * then the installation state changed and we need to clear the UI for the modifications to be visible.
	 */
	private boolean installationChanged() {
		try {
			final Bundle bundle = FrameworkUtil.getBundle(getClass());
			final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(bundle.getSymbolicName());
			final String savedState = node.get(INSTALLATION_STATE_KEY, EMPTY_STRING);
			final String currentState = currentInstallationState(bundle, node);
			node.put(INSTALLATION_STATE_KEY, currentState);
			node.flush();
			
			if (isEmpty(savedState))
				// initial app startup or after cleanup, we don't need to clear
				return false;
			
			return !savedState.equalsIgnoreCase(currentState);
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Returns the current installation state. Basically a list of all Linic UI plugin symbolic names and version.
	 * We only return plugins that contain UI elements, so pure code plugins, such as wrapped.ro.linic.util.commons 
	 * or ro.linic.ui.http are ignored.
	 */
	private String currentInstallationState(final Bundle bundle, final IEclipsePreferences node) {
		final Set<String> installedBundles = new HashSet<>();
		for (final Bundle b : bundle.getBundleContext().getBundles()) {
			if (!b.getSymbolicName().startsWith("ro.linic.ui"))
				continue;
			if (NON_UI_PLUGINS.contains(b.getSymbolicName()))
				continue;
			
			installedBundles.add(b.getSymbolicName() + '_' + b.getVersion());
		}
		
		return installedBundles.stream()
				.sorted()
				.collect(Collectors.joining(LIST_SEPARATOR));
	}

	@Override
	public void save() throws IOException {
		if (saveAndRestore) {
			final Map<String, Object> options = new HashMap<>();
			options.put(E4XMIResource.OPTION_FILTER_PERSIST_STATE, Boolean.TRUE);
			resource.save(options);
		}
	}

	/**
	 * Creates a resource with an app Model, used for saving copies of the main app model.
	 *
	 * @param theApp
	 *            the application model to add to the resource
	 * @return a resource with a proper save path with the model as contents
	 */
	@Override
	public Resource createResourceWithApp(final MApplication theApp) {
		final Resource res = createResource();
		res.getContents().add((EObject) theApp);
		return res;
	}

	private Resource createResource() {
		if (saveAndRestore) {
			final URI saveLocation = URI.createFileURI(getWorkbenchSaveLocation().getAbsolutePath());
			return resourceSet.createResource(saveLocation);
		}
		return resourceSet.createResource(URI.createURI("workbench.xmi")); //$NON-NLS-1$
	}

	private File getWorkbenchSaveLocation() {
		return new File(getBaseLocation(), "workbench.xmi"); //$NON-NLS-1$
	}

	private File getBaseLocation() {
		File baseLocation;
		try {
			baseLocation = new File(URIUtil.toURI(instanceLocation.getURL()));
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
		baseLocation = new File(baseLocation, ".metadata"); //$NON-NLS-1$
		baseLocation = new File(baseLocation, ".plugins"); //$NON-NLS-1$
		return new File(baseLocation, "org.eclipse.e4.workbench"); //$NON-NLS-1$
	}

	// Ensures that even models with error are loaded!
	private Resource loadResource(final URI uri) {
		Resource resource;
		try {
			resource = getResource(uri);
		} catch (final Exception e) {
			// TODO We could use diagnostics for better analyzing the error
			logger.error(e, "Unable to load resource " + uri); //$NON-NLS-1$
			return null;
		}

		// TODO once we switch from deltas, we only need this once on the default model?
		final String contributorURI = URIHelper.EMFtoPlatform(uri);
		if (contributorURI != null) {
			final TreeIterator<EObject> it = EcoreUtil.getAllContents(resource.getContents());
			while (it.hasNext()) {
				final EObject o = it.next();
				if (o instanceof MApplicationElement) {
					((MApplicationElement) o).setContributorURI(contributorURI);
				}
			}
		}
		return resource;
	}

	private Resource getResource(final URI uri) throws Exception {
		Resource resource;
		if (saveAndRestore) {
			resource = resourceSet.getResource(uri, true);
		} else {
			// Workaround for java.lang.IllegalStateException: No instance data can be specified
			// thrown by org.eclipse.core.internal.runtime.DataArea.assertLocationInitialized
			// The DataArea.assertLocationInitialized is called by ResourceSetImpl.getResource(URI,
			// boolean)
			resource = resourceSet.createResource(uri);
			resource.load(new URL(uri.toString()).openStream(), resourceSet.getLoadOptions());
		}

		return resource;
	}

	protected long getLastApplicationModification() {
		long appLastModified = 0L;
		final ResourceSetImpl resourceSetImpl = new ResourceSetImpl();

		final Map<String, ?> attributes = resourceSetImpl.getURIConverter().getAttributes(
				applicationDefinitionInstance,
				Collections.singletonMap(URIConverter.OPTION_REQUESTED_ATTRIBUTES,
						Collections.singleton(URIConverter.ATTRIBUTE_TIME_STAMP)));

		final Object timestamp = attributes.get(URIConverter.ATTRIBUTE_TIME_STAMP);
		if (timestamp instanceof Long) {
			appLastModified = ((Long) timestamp).longValue();
		} else if (applicationDefinitionInstance.isPlatformPlugin()) {
			try {
				final java.net.URL url = new java.net.URL(applicationDefinitionInstance.toString());
				// can't just use 'url.openConnection()' as it usually returns a
				// PlatformURLPluginConnection which doesn't expose the
				// last-modification time. So we try to resolve the file through
				// the bundle to obtain a BundleURLConnection instead.
				final Object[] obj = PlatformURLPluginConnection.parse(url.getFile().trim(), url);
				final Bundle b = (Bundle) obj[0];
				// first try to resolve as an bundle file entry, then as a resource using
				// the bundle's classpath
				java.net.URL resolved = b.getEntry((String) obj[1]);
				if (resolved == null) {
					resolved = b.getResource((String) obj[1]);
				}
				if (resolved != null) {
					final URLConnection openConnection = resolved.openConnection();
					appLastModified = openConnection.getLastModified();
				}
			} catch (final Exception e) {
				// ignore
			}
		}

		return appLastModified;
	}
}
