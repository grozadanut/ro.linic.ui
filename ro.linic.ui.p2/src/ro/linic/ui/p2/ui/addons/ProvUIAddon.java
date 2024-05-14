package ro.linic.ui.p2.ui.addons;

import java.net.URL;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.ui.LoadMetadataRepositoryJob;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Controls the lifecycle of the provisioning UI bundle
 *
 * @since 3.4
 */
public class ProvUIAddon {
	public static final String PLUGIN_ID = "ro.linic.ui.p2"; //$NON-NLS-1$
	public static final String IMG_REGISTRY = PLUGIN_ID + ".imageRegistry";  //$NON-NLS

	@PostConstruct
	void start(final IEclipseContext ctx, final IProvisioningAgent agent) {
		registerImageRegistry(ctx);
		registerProvisioningUI(ctx, agent);
	}

	@PreDestroy
	public void stop(final IEclipseContext ctx)
			throws OperationCanceledException, InterruptedException {
		try {
			// cancel any repository load jobs started in the UI
			Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=305163
			// join the jobs so that this bundle does not stop until the jobs are
			// actually cancelled.
			Job.getJobManager().join(LoadMetadataRepositoryJob.LOAD_FAMILY, new NullProgressMonitor());
		} finally {
			ctx.remove(ProvisioningUI.class);
			final ImageRegistry imageRegistry = (ImageRegistry) ctx.get(IMG_REGISTRY);
			if (imageRegistry != null)
				imageRegistry.dispose();
			ctx.remove(IMG_REGISTRY);
		}
	}
	
	private void registerImageRegistry(final IEclipseContext ctx) {
		final ImageRegistry imageRegistry = createImageRegistry();
		initializeImageRegistry(imageRegistry);
		ctx.set(IMG_REGISTRY, imageRegistry);
	}
	
	private void registerProvisioningUI(final IEclipseContext ctx, final IProvisioningAgent agent) {
		final ProvisioningSession session = new ProvisioningSession(agent);
		Policy policy = ctx.get(Policy.class);
		if (policy == null)
			policy = new Policy(ctx);
		final ProvisioningUI ui = new ProvisioningUI(ctx, session, IProfileRegistry.SELF, policy);
		ctx.set(ProvisioningUI.class, ui);
	}
	
	/**
	 * Returns a new image registry for this plugin-in. The registry will be used to
	 * manage images which are frequently used by the plugin-in.
	 *
	 * @return ImageRegistry the resulting registry.
	 * @see #getImageRegistry
	 */
	private ImageRegistry createImageRegistry() {

		// If we are in the UI Thread use that
		if (Display.getCurrent() != null) {
			return new ImageRegistry(Display.getCurrent());
		}

		// Invalid thread access if it is not the UI Thread
		// and the workbench is not created.
		throw new SWTError(SWT.ERROR_THREAD_INVALID_ACCESS);
	}

	private void initializeImageRegistry(final ImageRegistry reg) {
		final Bundle b = FrameworkUtil.getBundle(getClass());  
		createImageDescriptor(ProvUIImages.IMG_METADATA_REPOSITORY, reg, b);
		createImageDescriptor(ProvUIImages.IMG_ARTIFACT_REPOSITORY, reg, b);
		createImageDescriptor(ProvUIImages.IMG_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_DISABLED_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_UPDATED_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_ADDED_OVERLAY, reg, b);
		createImageDescriptor(ProvUIImages.IMG_REMOVED_OVERLAY, reg, b);
		createImageDescriptor(ProvUIImages.IMG_UPGRADED_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_DOWNGRADED_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_PATCH_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_DISABLED_PATCH_IU, reg, b);
		createImageDescriptor(ProvUIImages.IMG_CATEGORY, reg, b);
		createImageDescriptor(ProvUIImages.IMG_PROFILE, reg, b);
		createImageDescriptor(ProvUIImages.IMG_INFO, reg, b);
		createImageDescriptor(ProvUIImages.IMG_ADDED, reg, b);
		createImageDescriptor(ProvUIImages.IMG_REMOVED, reg, b);
		createImageDescriptor(ProvUIImages.IMG_CHANGED, reg, b);
		createImageDescriptor(ProvUIImages.IMG_NOTADDED, reg, b);
		createImageDescriptor(ProvUIImages.IMG_COPY, reg, b);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL, reg, b);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_REVERT, reg, b);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL, reg, b);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE, reg, b);
	}

	/**
	 * Creates the specified image descriptor and registers it
	 */
	private void createImageDescriptor(final String id, final ImageRegistry reg, final Bundle b) {
		final URL url = FileLocator.find(b, IPath.fromOSString(ProvUIImages.ICON_PATH + id), null);
		final ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		reg.put(id, desc);
	}
}
